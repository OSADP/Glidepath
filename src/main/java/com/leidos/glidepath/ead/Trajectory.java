package com.leidos.glidepath.ead;

import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.asd.map.MapMessage;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.Logger;
import com.leidos.glidepath.logger.LoggerManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;

public class Trajectory implements ITrajectory {
	
	/** Connects to the EAD algorithm library and initializes it for first use.
	 * config param ead.trajectoryfile not empty : initialize file for reading
	 * config param ead.trajectoryfile is empty  : pass max accel, max jerk, speed limit & time step duration into the EAD library for future reference
	 * @throws IOException 
	 */
	public Trajectory() throws Exception{
		
		//determine if we are going to use the EAD library or read a trajectory from a data file
		AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
		assert(config != null);
		csvFilename_ = config.getProperty("ead.trajectoryfile");
		timeStepSize_ = (long)config.getPeriodicDelay();
		
		//determine if we will be using the timeouts or allowed to run slow (for testing)
		respectTimeouts_ = Boolean.valueOf(config.getProperty("performancechecks"));
		log_.infof("EAD", "time step = %d ms, respecting timeouts = %b", timeStepSize_, respectTimeouts_);

		//get constraint parameters from the config file
		speedLimit_ = config.getMaximumSpeed()/Constants.MPS_TO_MPH; //max speed is the only parameter in the config file that is in English units!
		maxJerk_ = Double.valueOf(config.getProperty("maximumJerk"));
		log_.infof("EAD", "speedLimit = %.2f, maxJerk = %.2f", speedLimit_, maxJerk_);
		log_.info("EAD", "Acceleration limits are now situational; looked up for each time step");

		//get the acceleration & jerk limiter desires
		accelLimiter_	= Integer.valueOf(config.getProperty("ead.accelerationlimiter"));
		jerkLimiter_	= Integer.valueOf(config.getProperty("ead.jerklimiter"));
		
		//get the operating speed parameters
		osDuration_ = (long)(1000.0 * Double.valueOf(config.getProperty("ead.osduration")));
		trajStartDist_ = Double.valueOf(config.getProperty("ead.start.distance"));
		if (trajStartDist_ <= 0.0) {
			trajStartDist_ = Double.MAX_VALUE; //zero value implies infinity (start trajectory immediately)
		}
		rampupLimit_ = config.getProperty("ead.rampup.limit").equals("true");
		log_.infof("EAD", "Acceleration limiter = %d, jerk limiter = %d, osDuration = %d ms, start dist = %.1f, rampupLimit = %b", 
					accelLimiter_, jerkLimiter_, osDuration_, trajStartDist_, rampupLimit_);
		
		//get the max allowable spat error count
		maxSpatErrors_ = Integer.valueOf(config.getProperty("ead.max.spat.errors"));
		log_.infof("EAD", "Max spat errors = %d", maxSpatErrors_);
		
		//determine if we will be using HMI control instead of controlling the XGV
		hmiControl_ = config.getProperty("ucr.enabled").equals("true");
		log_.infof("EAD", "HMI control = %b", hmiControl_);
		
		//get failsafe parameters
		allowFailSafe_			= config.getProperty("ead.failsafe.on").equals("true");
		failSafeDistBuf_		= Double.valueOf(config.getProperty("ead.failsafe.distance.buffer"));
		failSafeResponseLag_	= Double.valueOf(config.getProperty("ead.failsafe.response.lag"));
		failSafeDecelFactor_	= Double.valueOf(config.getProperty("ead.failsafe.decel.factor"));
		log_.infof("EAD", "allowFailSafe = %b, failSafeDistBuf = %.2f, failSafeResponseLag = %.2f", allowFailSafe_, failSafeDistBuf_, failSafeResponseLag_);
		maxCmdAdj_		= Double.valueOf(config.getProperty("ead.maxcmdadj"));
		cmdAccelGain_	= Double.valueOf(config.getProperty("ead.cmdaccelgain"));
		cmdSpeedGain_	= Double.valueOf(config.getProperty("ead.cmdspeedgain"));
		cmdBias_		= Double.valueOf(config.getProperty("ead.cmdbias"));
		log_.infof("EAD", "maxCmdAdj = %.2f, cmdAccelGain = %.4f, cmdSpeedGain = %.4f, cmdBias = %.4f",
					maxCmdAdj_, cmdAccelGain_, cmdSpeedGain_, cmdBias_);
		
		//if a trajectory filename is specified then use the file
		if (csvFilename_ != null  &&  csvFilename_.length() > 0) {
			log_.debugf("EAD", "Attempting to use trajectory file %s", csvFilename_);
	        startCsvFile();
	        ead_ = null;

	    //else prepare to use and EAD object
		}else {
			//get the desired EAD variant from the config file and instantiate it
			String eadClass = config.getProperty("ead.modelclass");
			if (eadClass == null) {
				eadClass = "default";
			}
			ead_ = EadFactory.newInstance(eadClass);
			if (ead_ == null) {
				log_.errorf("EAD", "Could not instantiate the EAD model %s", eadClass);
				throw new Exception("Could not instantiate an EAD model.");
			}
			
			//pass config parameters to the EAD library
			int opt = 0;
			try {
				String logOption = config.getProperty("ead.librarylog");
				if (logOption != null) {
					if (logOption.equals("stdout")) {
						opt = 2;
					}else if (logOption.length() > 0) { //can't specify filename, so anything here will go to the same file
						opt = 1;
					}
				}
				ead_.initialize(timeStepSize_, opt);
			} catch (Exception e) {
				log_.errorf("EAD", "Exception thrown by EAD library initialize(). maxJerk = %f, speedLimit = %f", maxJerk_, speedLimit_);
				throw e;
			}
			
			csvParser_ = null;
			log_.infof("", "2. EADlib initialized. speedLimit = %.2f, maxJerk = %.2f, log opt = %d", speedLimit_, maxJerk_, opt);
		}
		
		//initialize other members
		prevCmd_ = 0.0;
		curSpeed_ = 0.0;
		prevSpeed_ = 0.0;
		curAccel_ = 0.0;
		eadErrorCount_ = 0;
		spatErrorCounter_ = 0;
		firstMotion_ = false;
		timeSinceFirstMotion_ = 0.0;
		timeOfFirstMotion_ = 0;
		stoppedCount_ = 0;
		stopConfirmed_ = false;
		operSpeedAchieved_ = false;
		timeOsAchieved_ = 0;
		intersectionInitialized_ = false;
		intersection_ = null;
		map_ = null;
		failSafeMode_ = false;
		numStepsAtZero_ = 0;
		motionAuthorized_ = false;
		firstTrajStep_ = true;
		accelMgr_ = AccelerationManager.getManager();
	}
	
	/**
	 * always : invoke the native EADlib shutdown function
	 */
	public void close() {
		//deprecated
	}
	
	/**
	 * always : indicate that this software is now authorized to control the vehicle's speed
	 */
	public void engage() {
		motionAuthorized_ = true;
		log_.info("EAD", "///// Driver engaged automatic control.");
	}
	
	/**config param ead.trajectoryfile not empty : read command and DTSB from specified file, 
	 * config param ead.trajectoryfile is empty && (state contains valid MAP message || valid MAP previously received) &&
	 * 		intersection geometry fully decomposed  &&  vehicle position is associated with a single lane  &&
	 * 			signal is red && (-W < distance to intersection < 0) : command = 0, computed DTSB
	 * 			oper speed never reached : command = oper speed, computed DTSB
	 * 			oper speed has been reached : speed command from EAD library, computed DTSB
	 * 		intersection not fully decomposed  ||  vehicle cannot be associated with a single lane : no speed command, no DTSB
	 * 	config param ead.trajectoryfile is empty  &&  no valid MAP has ever been received : no speed command, no DTSB
	 * 
	 * Note: DTSB = distance to stop bar; the "no speed command" result means that a DataElementHolder will be constructed 
	 * and returned without a SPEED_COMMAND element in it. Similar for "no DTSB".
	 * 
	 * @param state contains current SPEED, OPERATING_SPEED, LATITUDE (vehicle), LONGITUDE (vehicle), SIGNAL_PHASE, 
	 *        SIGNAL_TIME_TO_NEXT_PHASE, SIGNAL_TIME_TO_THIRD_PHASE, MAP_MESSAGE elements that we need here (it may
	 *        also contain other elements)
	 * 
	 * @return DataElementHolder containing SPEED_COMMAND
	 * @throws Exception 
	 */
	public DataElementHolder getSpeedCommand(DataElementHolder state) throws Exception {
		long entryTime = System.currentTimeMillis();
		DataElementHolder rtn = new DataElementHolder();

		DoubleDataElement curSpeedElement = null;
		DoubleDataElement curAccelElement = null;
		DoubleDataElement operSpeed = null;
		DoubleDataElement vehicleLat = null;
		DoubleDataElement vehicleLon = null;
		MapMessageDataElement newMap = null;
		PhaseDataElement phase = null;
		DoubleDataElement time1 = null;
		DoubleDataElement time2 = null;
		
		//extract input data and check that all values are from the same time step (don't check age of MAP or operating speed
		// because may not be present every time step; don't check accel or jerk because they are packaged with smoothed speed)
		curSpeedElement = (DoubleDataElement)state.get(DataElementKey.SMOOTHED_SPEED);
		curAccelElement = (DoubleDataElement)state.get(DataElementKey.ACCELERATION);
		operSpeed = (DoubleDataElement)state.get(DataElementKey.OPERATING_SPEED);
		vehicleLat = (DoubleDataElement)state.get(DataElementKey.LATITUDE);
		vehicleLon = (DoubleDataElement)state.get(DataElementKey.LONGITUDE);
		newMap = (MapMessageDataElement)state.get(DataElementKey.MAP_MESSAGE);
		phase = (PhaseDataElement)state.get(DataElementKey.SIGNAL_PHASE);
		time1 = (DoubleDataElement)state.get(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE);
		time2 = (DoubleDataElement)state.get(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE);
		validateElement(curSpeedElement, "curSpeedElement", true);
		validateElement(curAccelElement, "curAccelElement", true);
		validateElement(operSpeed, "operSpeed", true);
		validateElement(vehicleLat, "vehicleLat", true);
		validateElement(vehicleLon, "vehicleLon", true);
		
		//it's a very serious problem if the above items don't validate, so let those exceptions bubble up.
		// for the SPAT data, it is not uncommon to miss a message now and then, so we need to extrapolate them if we can
		try {
			validateElement(phase, "phase", false);
			validateElement(time1, "time1", false);
			validateElement(time2, "time2", false);
			spatErrorCounter_ = 0;
		} catch (Exception e1) {
			if (++spatErrorCounter_ > maxSpatErrors_) {
				log_.errorf("EAD", "getSpeedCommand: fatal error missing SPAT data for %d consecutive time steps", spatErrorCounter_);
				throw e1;
			}
			double step = (double)timeStepSize_ / 1000.0;
			if (prevTime1_ >= step) { //if we're changing phases now, we don't know what the new times will be
				phase = new PhaseDataElement(prevPhase_);
				time1 = new DoubleDataElement(prevTime1_ - step);
				time2 = new DoubleDataElement(prevTime2_ - step);
			}else {
				phase = new PhaseDataElement(prevPhase_.next());
				time1 = new DoubleDataElement(prevTime2_ - step);
				time2 = new DoubleDataElement(prevTime2_ - step + 3.0); //take a wild guess; should get real data in next time step
			}
		}
		//save the values for the next time step
		prevPhase_ = phase.value();
		prevTime1_ = time1.value();
		prevTime2_ = time2.value();
		
		if (respectTimeouts_) {
			long oldestTime = curSpeedElement.timeStamp();
			if (vehicleLat.timeStamp() < oldestTime) oldestTime = vehicleLat.timeStamp();
			if (vehicleLon.timeStamp() < oldestTime) oldestTime = vehicleLon.timeStamp();
			if (phase.timeStamp() < oldestTime) oldestTime = phase.timeStamp();
			if (time1.timeStamp() < oldestTime) oldestTime = time1.timeStamp();
			if (time2.timeStamp() < oldestTime) oldestTime = time2.timeStamp();
			if ((entryTime - oldestTime) > 0.9*timeStepSize_) { //allow time for this method to execute within the time step
				log_.errorf("EAD", "getSpeedCommand detects stale input data. curTime = %d, oldestTime = %d, timeStepSize_ = %d",
							entryTime, oldestTime, timeStepSize_);
				log_.errorf("EAD", "    speed is            %5d ms old", entryTime-curSpeedElement.timeStamp());
				log_.errorf("EAD", "    latitude is         %5d ms old", entryTime-vehicleLat.timeStamp());
				log_.errorf("EAD", "    longitude is        %5d ms old", entryTime-vehicleLon.timeStamp());
				log_.errorf("EAD", "    phase is            %5d ms old", entryTime-phase.timeStamp());
				log_.errorf("EAD", "    time next phase is  %5d ms old", entryTime-time1.timeStamp());
				log_.errorf("EAD", "    time third phase is %5d ms old", entryTime-time2.timeStamp());
			}
		}

		//store input info in a more usable format
		phase_ = phase.value();
		timeRemaining_ = time1.value();
		operSpeed_ = operSpeed.value();
		curSpeed_ = curSpeedElement.value(); //this is the smoothed value
		curAccel_ = curAccelElement.value(); //smoothed
		
		//if we have begun moving then
		if (firstMotion_) {
			//if we are still in motion then
			if (curSpeed_ > 0.03) { //allow for noise
				//clear the stop counter
				stoppedCount_ = 0;

			//else if we are at a confirmed stop then
			}else if (stopConfirmed_) {
					//release that constraint only when the signal turns green
					if (phase_.equals(SignalPhase.GREEN)) {
						stopConfirmed_ = false;
						stoppedCount_ = 0;
						log_.debug("EAD", "Clearing stopConfirmed flag - signal now green.");
					}

			//else we are tentatively stopped at red and need to make sure it isn't just noise in the speed data
			}else if (!phase_.equals(SignalPhase.GREEN)){
				//if we have been at rest for several consecutive time steps then (must be waiting at the red light)
				if (++stoppedCount_ > 14) { //sometimes takes 9-12 time steps after a cmd before vehicle start moving
					//indicate a confirmed stop
					stopConfirmed_ = true;
					//clear the motion authorization
					motionAuthorized_ = false;
					log_.info("EAD", "Clearing motionAuthorized flag - too long without motion");
				}
			}

		//else if speed is positive then
		}else if (curSpeed_ > 0.03) { //avoid some weird noise whilst sitting on starting line
			//indicate that we have begun moving
			firstMotion_ = true;
			timeOfFirstMotion_ = System.currentTimeMillis();
			log_.info("EAD", "///// FIRST MOTION DETECTED /////");
		}

		//process any new MAP message that may have come in
		handleNewMap(newMap);
		
		//if the intersection object is fully initialized then
		if (intersectionInitialized_) {
			double cmd = 0.0;
			double dist = -1.0; //need this to be negative so the fail-safe won't jam the brakes if intersection geometry fails
		
			//compute the current vehicle geometry relative to the intersection
			boolean associatedWithLane = intersection_.computeGeometry(vehicleLat.value(), vehicleLon.value());
			
			//if the computation was successful (vehicle can be associated with a lane) then
			if (associatedWithLane) {
				
				//get the DTSB and lane ID (not the index)
				dist = intersection_.dtsb();
				int laneId = intersection_.laneId();
				
				//if the driver has turned control over to the computer and it is lawful to proceed, or we are in HMI operations then
				if ((motionAuthorized_  &&  !stopConfirmed_)  ||  hmiControl_) {
				
					//if we have never achieved user's desired operating speed and the current speed is still below the operating speed then
					// (we are in the initial acceleration phase)
					if (!operSpeedAchieved_  &&  curSpeed_ < operSpeed.value()) {
						double os = operSpeed.value();
						
						//tell the acceleration manager that we're ramping up
						accelMgr_.currentScenarioIs(Scenario.RAMP_UP, curSpeed_, os);
						
						//if we want to limit the ramp-up acceleration (independent of the global accel limiter) then
						if (rampupLimit_) {
							//set the new speed command to the actual speed + allowed delta; limit it to slightly above the operating speed
							double delta = 0.001*(double)timeStepSize_*accelMgr_.getAccelLimit();
							cmd = prevCmd_ + delta;
							if (curSpeed_ > os - 1.0 ) { //crude tapering to minimize overshoot
								cmd = prevCmd_ + 0.5*delta;
							}
							if (cmd > os + 0.5) {
								cmd = os + 0.5;
							}						
						//else (tell XGV to accelerate as fast as it can)
						}else {
							cmd = os + 0.5; //aim above the target because we will tangentially approach it
						}
						log_.infof("EAD", "3. Operating speed not yet achieved. Oper speed = %.2f,  cur speed = %.2f, cmd = %.2f", os, curSpeed_, cmd);
					
					//else if we are using a trajectory file for simulation then
					}else if (csvParser_ != null) {
	
						//indicate that we have achieved operating speed
						indicateOperatingSpeed();
						cmd = operSpeed_;
				
						//if we have settled into the operating speed and reached the configured starting line then
						if (isTrajectoryActive(dist)) {
							//get the next command from the CSV file
							cmd = getCommandFromCsv();
						}
						
					//else invoke the EAD algorithm
					}else {
					
						//indicate that we have achieved operating speed
						indicateOperatingSpeed();
						cmd = operSpeed_;
						
						//allow the vehicle to settle into the operating speed and cross the starting line before turning on EAD
						if (isTrajectoryActive(dist)) {
				
							try {
								//update the EAD library with the current state data
								int ph = phase.value().value();
								ead_.setState(curSpeed_, operSpeed.value(), curAccel_, dist, ph, time1.value(), time2.value());
								
								//pass the stop box width to the EAD library
								ead_.setStopBoxWidth(intersection_.stopBoxWidth());
		
								//invoke the EAD library to calculate the new speed command
								cmd = ead_.getTargetSpeed();
								log_.infof("EAD", "5. Using routine speed command from EADlib: %.2f", cmd);
			
							} catch (Throwable e) {
								if (eadErrorCount_++ < MAX_ERROR_COUNT) {
									cmd = prevCmd_;
									log_.warnf("EAD", "Exception trapped from EAD library. Continuing to use previous command." +
											"curSpeed = %.2f, operSpeed = %.2f, dist = %.2f, time1 = %.2f, time2 = %.2f",
											curSpeed_, operSpeed.value(), dist, time1.value(), time2.value());
								}else {
									log_.errorf("EAD", "Exception thrown by EAD library. Rethrowing." +
											"curSpeed = %.2f, operSpeed = %.2f, dist = %.2f, time1 = %.2f, time2 = %.2f",
											curSpeed_, operSpeed.value(), dist, time1.value(), time2.value());
									throw e;
								}
							}
						}
	
					} //endif invoke EAD algorithm
				} //endif driver turned over control
				
				//assemble the output with new lane ID, and DTSB
				DoubleDataElement dtsb = new DoubleDataElement(dist);
				rtn.put(DataElementKey.DIST_TO_STOP_BAR, dtsb);
				IntDataElement lane = new IntDataElement(laneId);
				rtn.put(DataElementKey.LANE_ID, lane);
			
			//else (could not associate to a lane)
			}else {
				//continue the command from the previous time step
				cmd = prevCmd_;
				
				//add a status message for the DVI so the operator knows there is a problem
				String msg = "Lane geometry error; DTSB is unknown; assuming speed command from previous time step.";
				log_.warn("EAD", msg);
				StringBuffer sb = new StringBuffer(msg);
				StringBufferDataElement sbe = new StringBufferDataElement(sb);
				rtn.put(DataElementKey.STATUS_MESSAGE, sbe);
			} //endif geometry computation was successful
			
			//apply acceleration & jerk limits to the command
			cmd = limitSpeedCommand(cmd);
			
			//invoke the fail-safe command override to ensure we don't run a red light even if all logic above fails
			cmd = applyFailSafeCheck(cmd, dist, curSpeed_);

			//preserve history for the next time step
			prevCmd_ = cmd;
			prevSpeed_ = curSpeed_;
			if (firstMotion_) {
				timeSinceFirstMotion_ = 0.001*(double)(System.currentTimeMillis() - timeOfFirstMotion_);
			}
			DoubleDataElement tfm = new DoubleDataElement(timeSinceFirstMotion_);
			rtn.put(DataElementKey.TIME_SINCE_FIRST_MOTION, tfm);
			
			//add the speed command to the return holder
			DoubleDataElement speedCmd = new DoubleDataElement(cmd);
			rtn.put(DataElementKey.SPEED_COMMAND, speedCmd);
			log_.debugf("EAD", "7. getSpeedCommand exiting. Commanded speed is %.3f m/s. curSpeed = %.3f m/s, method time = %d ms.", 
					cmd, curSpeed_, System.currentTimeMillis()-entryTime);
			
			//if failsafe was invoked add a DVI status message to indicate that
			/***** disabled for now, but probably want to bring this back when a debug mode is added to the DVI
			if (failSafeMode_) {
				String msg = "Fail-safe command override has been invoked.";
				StringBuffer sb = new StringBuffer(msg);
				StringBufferDataElement sbe = new StringBufferDataElement(sb);
				rtn.put(DataElementKey.STATUS_MESSAGE, sbe);
			}
			*****/

		} //endif intersection is initialized

        Duration duration = new Duration(new DateTime(entryTime), new DateTime());
        rtn.put(DataElementKey.CYCLE_EAD, new IntDataElement((int) duration.getMillis()));

        return rtn;
	}


	//////////////////
	// member elements
	//////////////////
	
	
	/**
	 * always : elem != null
	 * 
	 * if element is null, then write a log warning.
	 * @throws Exception 
	 */
	private boolean validateElement(Object elem, String name, boolean logIt) throws Exception {
		if (elem == null) {
			if (logIt) log_.warnf("EAD", "Critical input state element is null:  %s", name);
			throw new Exception("Critical input state element is null: " + name);
		}
		return elem != null;
	}
	/**
	 * newMap exists and is different from previously stored map : initialize intersection geometry model from it
	 * else : do nothing
	 */
	private void handleNewMap(MapMessageDataElement newMap) {
		//if the there is an incoming MAP message and it is different from the one we were working with in previous time step then
		if (newMap != null  &&  (map_ == null  ||  newMap.value().intersectionId() != map_.intersectionId()  ||  newMap.value().getContentVersion() != map_.getContentVersion())) {
			//create a new intersection object and indicate that it has not been initialized
			map_ = newMap.value();
			intersection_ = new Intersection(respectTimeouts_);
			intersectionInitialized_ = false;
		}
		
		//if the intersection model isn't initialized then 
		// Note: need this logic in its own block because
		// a) we won't see a MAP every time, so it can't be combined with above
		// b) this may take longer than one time step to complete, so can't force it to finish before moving on
		if (!intersectionInitialized_) {
			
			//initialize it
			try {
				if (map_ != null) {
					intersectionInitialized_ = intersection_.initialize(map_);
				}
			} catch (Exception e) {
				//this should never happen because we are verifying above that the map hasn't changed
				log_.errorf("EAD", "Exception trapped in getSpeedCommand: %s", e.toString());
			}
		}
	}

	/**
	 * csvFilename_ represents a valid trajectory file : open file and set up an iterator on it
	 */
	private void startCsvFile() throws IOException {
		try  {
		    File csv = new File(csvFilename_);
		    csvParser_ = CSVParser.parse(csv, Charset.forName("UTF-8"), CSVFormat.RFC4180);
		    iter_ = csvParser_.iterator();
		    log_.warnf("EAD", "Opened trajectory file %s", csvFilename_);
		} catch (IOException e) {
			log_.errorf("EAD", "Cannot open CSV test file %s", csvFilename_);
			throw e;
		}
	}

	/**
	 * csv file has records remaining : get speed command from next record
	 * csv file is on last record : rewind the file and get speed command from its first record
	 */
	private double getCommandFromCsv() {
		double cmd;
		try  {
			//read the next record from the file for our command
			CSVRecord rec = iter_.next();
			//double time = Double.parseDouble(rec.get(0).trim()); //don't need this now, but it's in the data file
			cmd = Double.parseDouble(rec.get(1).trim());
			//dist = Double.parseDouble(rec.get(2).trim());
			//laneId = Integer.parseInt(rec.get(3).trim());
		} catch (Exception e) {
			//use the last command on the file for one more time step while we rewind the file
			cmd = prevCmd_;
			log_.info("EAD", "No more commands in trajectory file. Preparing to rewind.");

			//close the file, open it again, and start all over
			try {
				csvParser_.close();
				startCsvFile();
			} catch (IOException e1) {
				log_.error("EAD", "Failed to re-open the trajectory file: " + e1.toString());
				e1.printStackTrace();
			}
		}
		return cmd;
	}

	/**
	 * always : set operating speed flag
	 * first time that operating speed has been achieved : add log notification
	 */
	private void indicateOperatingSpeed() {
		//indicate that we have achieved operating speed (we touched it for at least one time step)
		operSpeedAchieved_ = true;
		if (timeOsAchieved_ == 0) {
			timeOsAchieved_ = System.currentTimeMillis();
			log_.info("EAD", "////////// 4. Achieved operating speed");
		}
	}
	
	/**
	 * DTSB <= start distance && time since OS achieved >= OS duration : true
	 * else : false
	 * 
	 * Note: if start distance is specified as <= 0 then the DTSB part of the test will always return true
	 */
	private boolean isTrajectoryActive(double dist) {
		boolean rtn = true;
		
		//test the time spent at operating speed
		if (System.currentTimeMillis() - timeOsAchieved_ < osDuration_) {
			rtn = false;
		
		//test the distance to stop bar
		}else if (dist > trajStartDist_) {
			rtn = false;
		}
		
		if (rtn == true  &&  firstTrajStep_) {
			firstTrajStep_ = false;
			log_.info("EAD", "///// Desired trajectory initiated.");
		}
		
		return rtn;
	}

	/**
	 * desiredSpeed > growth allowed by max accel or max jerk : limited to max allowed growth
	 * desiredSpeed < decay allowed by max decel or max jerk : limited to max allowed decay
	 * desiredSpeed within limits : desiredSpeed
	 */
	private double limitSpeedCommand(double desiredSpeed) {
		double command = desiredSpeed;
		double timeStep = 0.001*(double)timeStepSize_; //units of sec
		
		//apply accel/decel limits with instantaneous (smoothed) speed data
		if (accelLimiter_ == 1  ||  accelLimiter_ == 2) {
			double desiredSpeedDiff = desiredSpeed - curSpeed_;
			double highAccelLimit = accelMgr_.getAccelLimit()*timeStep;
			double lowAccelLimit = -highAccelLimit;
			if (desiredSpeedDiff > highAccelLimit) {
				command = curSpeed_ + highAccelLimit;
				log_.infof("EAD", "Target limited by positive acceleration. desiredSpeedDiff = %.3f. New cmd = %.3f", 
							desiredSpeedDiff, command);
			}else if (desiredSpeedDiff < lowAccelLimit) {
				command = curSpeed_ + lowAccelLimit;
				log_.infof("EAD", "Target limited by negative acceleration. desiredSpeedDiff = %.3f. New cmd = %.3f", 
							desiredSpeedDiff, command);
			}
		}			
		
		//apply jerk limits
		if (jerkLimiter_ > 0) {
			double curAccel = 0.0;
			
			//apply jerk limits with instantaneous (smoothed) speed data
			if (jerkLimiter_ == 1) {
				curAccel = (curSpeed_ - prevSpeed_)/timeStep;
			//else apply jerk limits with smoothed acceleration data
			}else if (jerkLimiter_ == 2) {
				curAccel = curAccel_;
			}
			double desiredAccelDiff = (command - curSpeed_)/timeStep - curAccel;
			double jerkLimit = maxJerk_*timeStep;
			if (desiredAccelDiff > jerkLimit) {
				command = curSpeed_ + (curAccel + maxJerk_*timeStep)*timeStep;
				log_.infof("EAD", "Target limited by positive jerk. curAccel = %.3f, desiredAccelDiff = %.3f. New cmd = %.3f", 
							curAccel, desiredAccelDiff, command);
			}else if (desiredAccelDiff < -jerkLimit) {
				command = curSpeed_ + (curAccel - maxJerk_*timeStep)*timeStep;
				log_.infof("EAD", "Target limited by negative jerk. curAccel = %.3f, desiredAccelDiff = %.3f. New cmd = %.3f", 
							curAccel, desiredAccelDiff, command);
			}
		}
			
		//final sanity check for legal commands
		if (command < 0.0) {
			command = 0.0;
		}else if (command >= speedLimit_) {
			command = speedLimit_ - 0.1;
		}
		
		return command;
	}

	/**
	 * current speed/distance state is beyond the safe limit for approaching red or yellow signal : command maximum deceleration
	 * current speed/distance state is in the safe zone for the signal state : cmdIn
	 * 
	 * Note: jerk limit is ignored here, as this is an emergency maneuver. Also, emergency command is intentionally
	 * not dependent upon current actual speed, because measured speed may be drifting in wrong direction; we know
	 * we have to bring the vehicle to a stop along a sharp downward speed trajectory in a given distance, so 
	 * distance is all that's necessary to know.
	 * 
	 * Note (3/1/15):  At this moment the XGV/vehicle responsiveness is only something we know empirically. Available data 
	 * indicates that it will achieve serious acceleration only when abs(command - speed) > 1 m/s.  Further, it takes
	 * somewhere around 1 to 1.5 sec to achieve a significant amount of torque (positive or negative) that begins to
	 * accelerate the vehicle in the desired way, even with an instantaneous 1 m/s command premium over actual speed.
	 */
	private double applyFailSafeCheck(double cmdIn, double distance, double speed) {
		double cmd = cmdIn;
		
		//if the failsafe toggle has been turned off then return
		if (!allowFailSafe_) {
			return cmd;
		}
		
		//set the acceleration limit higher than in normal ops since this is for handling emergency situations (this will be a positive number)
		double decel = failSafeDecelFactor_*accelMgr_.getAccelLimit();
		
		//compute the distance that will be covered during vehicle response lag
		double lagDistance = failSafeResponseLag_ * speed;
		
		//determine emergency stop distance for our current speed
		double emerDistance = 0.5*speed*speed / decel + lagDistance + failSafeDistBuf_;
		
		//are we in fail-safe mode?
		if (failSafe(emerDistance, distance, speed)) {
		
			//determine where we will be one time step in the future, going at the current speed (may be negative!)
			// plan to stop failSafeDistBuf_ short of the stop bar, and account for the response lag time
			double futureDistance = distance - 0.001*timeStepSize_*speed - lagDistance - failSafeDistBuf_;
			//determine the speed we want to have at that point in time to achieve a smooth slow-down
			double desiredSpeed = Math.sqrt(Math.max(2.0*decel*futureDistance, 0.0));
			
			//to avoid a big jerk at the bottom, if the desired speed is below crawling,
			// recalculate by gradually removing the lag distance (it has already been used up anyway)
			//---commenting out for evaluation before deciding on deletion for real:
			//if (desiredSpeed < CRAWLING) {
			//	log_.debugf("EAD", "Recalculating desiredSpeed. Was %.3f, lagDistance = %.3f", desiredSpeed, lagDistance);
			//	double factor = desiredSpeed/CRAWLING;
			//	desiredSpeed = Math.sqrt(Math.max(2.0*decel*(futureDistance + (1.0 - factor)*lagDistance), 0.0));
			//}
			
			//determine control adjustment that provides a command sufficiently below the actual speed that the controller will take it seriously
			double adj = calcControlAdjustment(speed, desiredSpeed, -decel);
			double rawCmd = desiredSpeed + adj;
		
			//compute the new fail-safe command
			double failsafeCmd = Math.max(Math.min(rawCmd, speed), 0.0);
			
			//under no circumstances do we want the resultant command to be larger than the previous one!
			if (failsafeCmd > prevCmd_) {
				failsafeCmd = 0.99*prevCmd_;
			}
			log_.debugf("EAD", "Fail-safe futureDistance = %.2f, speed = %.2f, desiredSpeed = %.2f, rawCmd = %.2f, adj = %.2f, failsafeCmd = %.2f",
					futureDistance, speed, desiredSpeed, rawCmd, adj, failsafeCmd);
			
			//if it is less than the input command then use it
			if (failsafeCmd < cmdIn) {
				cmd = failsafeCmd;
				log_.warn("EAD", "FAIL-SAFE has overridden the calculated command");
			}

		}
		
		return cmd;
	}

	/**
	 * !failSafeMode_  &&  distance < emerDist  &&  can't get through a green light at current speed : true, and set failSafeMode_
	 * failSafeMode_  &&  speed == 0 for 5 consecutive time steps : false, and clear failSafeMode_
	 * otherwise, return current failSafeMode_ without changing it
	 * 
	 * The idea is that once we decide we need a failsafe (emergency) stop, then we are committed to it until the deed is done.
	 */
	private boolean failSafe(double emerDist, double distance, double speed) {
		
		//if already in failsafe mode then
		if (failSafeMode_) {
			//if speed has been zero for 5 consecutive time steps then
			if (speed < 0.001  &&  ++numStepsAtZero_ > 5) {
				//turn off failsafe and trust the EAD to take over the departure
				failSafeMode_ = false;
				log_.info("EAD", "///// Fail-safe has been turned off.");
			}
		//else
		}else {
			//if we are still approaching and current DTSB < our emergency stop distance for this speed and speed is non-zero then
			if (distance > 0.0  &&  distance < emerDist  &&  speed > 0.1) {
		
				//determine time to cross stop bar at current speed
				double timeToCross = Math.max(distance/speed, 0.0);
			
				//determine if we have to stop (go into failsafe mode): if
				//	green and we can't make it through before expiring, or
				//	yellow, or
				//	red and it will still be red when we arrive
				failSafeMode_ = (phase_.equals(SignalPhase.GREEN)  &&  timeToCross >= timeRemaining_)  ||
								 phase_.equals(SignalPhase.YELLOW)                                     ||
								(phase_.equals(SignalPhase.RED)    &&  timeToCross <= timeRemaining_);
				if (failSafeMode_) {
					log_.warnf("EAD", "///// FAIL-SAFE ACTIVATED; will remain active until vehicle stops. timeToCross = %.2f", timeToCross);
				}
			}
		}

		return failSafeMode_;
	}
	
	/**
	 * always : adjustment to be added to the desired speed to get the final command
	 */
	private double calcControlAdjustment(double actSpeed, double desiredSpeed, double accel) {
		double p = cmdAccelGain_*(accel - curAccel_) + cmdSpeedGain_*(desiredSpeed - actSpeed) - cmdBias_; //note subtracting bias since we know we're slowing
		if (p > maxCmdAdj_) {
			p = maxCmdAdj_;
		}else if (p < -maxCmdAdj_) {
			p = -maxCmdAdj_;
		}
		
		return p;
	}
	
			
	private long				timeStepSize_;		//duration of a single time step, ms
	private boolean				motionAuthorized_;	//has the driver turned over control to the computer?
	private boolean				hmiControl_;		//are we using the 2012 HMI-to-driver feedback for vehicle control (instead of XGV commands)?
	private boolean				firstMotion_;		//have we detected first motion?
	private double				timeSinceFirstMotion_; //elapsed time (sec) since we detected the first motion
	private long				timeOfFirstMotion_;	//timestamp when first motion is detected (ms)
	private int					stoppedCount_;		//number of consecutive time steps that we have been stopped since first motion after being re-authorized
	private boolean				stopConfirmed_;		//is it confirmed that we have come to a complete stop?
	private double				operSpeed_;			//the operating speed, m/s
	private long				osDuration_;		//duration that we want to follow the operating speed before initiating EAD control, ms
	private double				trajStartDist_;		//DTSB threshold where we will start the trajectory commands or csv file commands, meters
	private boolean				firstTrajStep_;		//is this the first time step for the trajectory (or we are still waiting on it)?
	private boolean				rampupLimit_;		//do we want to limit acceleration on the initial ramp-up maneuver?
	private boolean				operSpeedAchieved_;	//soft latch; have we achieved operating speed at some point since vehicle started moving?
	private long				timeOsAchieved_;	//timestamp (ms since 1970) when we first achieved the operating speed
	private boolean				intersectionInitialized_; //has the intersection model been fully initialized?
	private boolean				respectTimeouts_;	//should we honor the established timeouts?
	private double				prevCmd_;			//speed command from the previous time step, m/s
	private SignalPhase			prevPhase_;			//signal phase in the previous time step
	private double				prevTime1_;			//signal time to end of current phase in previous time step, sec
	private double				prevTime2_;			//signal time to end of next phase in previous time step, sec
	private int					spatErrorCounter_;	//number of consecutive times spat data is missing
	private int					maxSpatErrors_;		//max allowable number of consecutive spat data errors
	private double				speedLimit_;		//upper limit allowed for speed, m/s
	private double				maxJerk_;			//max allowed jerk, m/s^3
	private double				curSpeed_;			//current vehicle speed, m/s
	private double				prevSpeed_;			//vehicle speed in previous time step, m/s
	private double				curAccel_;			//current acceleration (smoothed), m/s^2
	private SignalPhase			phase_;				//the signal's current phase
	private double				timeRemaining_;		//time remaining in the current signal phase, sec
	private String				csvFilename_;		//name of the CSV file used for dummy trajectory data
	private CSVParser			csvParser_;			//parser to read the CSV trajectory file
	private Iterator<CSVRecord>	iter_;				//iterator to be used on a CSV trajectory file
	private Intersection		intersection_;		//the current intersection that we are interacting with
	private MapMessage			map_;				//the MAP message that describes the current intersection
	private IEad				ead_;				//the EAD model that computes the speed commands
	private int					eadErrorCount_;		//number of exceptions thrown by the EAD library
	private int					accelLimiter_;		//controls how acceleration limiter is used; 0=none, 1=instantaneous speed data, 2=smoothed
	private int					jerkLimiter_;		//controls how jerk limiter is used; 0=none, 1=instantaneous speed data, 2=smoothed
	private double				maxCmdAdj_;			//difference (m/s) needed between current speed and command given to the XGV to get it to respond quickly
	private boolean				allowFailSafe_;		//does the user want failsafe logic involved?
	private boolean				failSafeMode_;		//are we in failsafe mode (overriding all other trajectory calculations)?
	private double				failSafeDistBuf_;	//distance buffer that failsafe will subtract from stop bar location, meters
	private double				failSafeResponseLag_; //time that failsafe logic allows for the vehicle to respond to a command change, sec
	private double				failSafeDecelFactor_; //Multiplier on the normal max allowed deceleration that should be used for failsafe
	private	int					numStepsAtZero_;	//number of consecutive time steps with speed of zero (after first motion)
	private double				cmdSpeedGain_;		//gain applied to speed difference for fail-safe calculations
	private double				cmdAccelGain_;		//gain applied to acceleration for fail-safe calculations
	private double				cmdBias_;			//bias applied to command premium for fail-safe calculations
	private AccelerationManager	accelMgr_;			//manages acceleration limits

	private static final int	MAX_ERROR_COUNT = 3;//max allowed number of EAD exceptions
	private static final double	CRAWLING = 2.5;		//crawling speed, m/s
	private static Logger		log_ = (Logger)LoggerManager.getLogger(Trajectory.class);
}
