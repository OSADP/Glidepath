package com.leidos.glidepath.ead;

import com.leidos.glidepath.appcommon.SignalPhase;
import com.leidos.glidepath.logger.Logger;
import com.leidos.glidepath.logger.LoggerManager;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;

/**
 * 
 * An alternative to the UCR-provided C code.  This code implements the UCR algorithm
 * directly from their white paper, dated 11/12/2014, and includes mods from the addendum
 * of 2/14/2015.
 * 
 * @author starkj
 *
 */

public class EadUcrJava extends EadBase implements IEad {
	
	/**
	 * Gets signal cycle data from config file and initializes necessary member variables.
	 */
	public EadUcrJava() {
		super();
		log_.info("EADL", "////////// Instantiating EadUcrJava object //////////");
	
		//get the speed error tolerance
		AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
		maxSpeedError_ = Double.valueOf(config.getProperty("ead.maxerror"));
		log_.infof("EADL", "Using maxSpeedError = %.1f", maxSpeedError_);
		
		//get the min DTSB for calculating a new approach scenario
		minScenarioDist_ = Double.valueOf(config.getProperty("ead.scenario.dist"));
		
		//get the safety distance calculation choice
		useOrigSafetyDist_ = Integer.valueOf(config.getProperty("ead.safety.dist")) == 0;
		log_.infof("EADL", "useOrigSafetyDist = %b; minScenarioDist = %.2f", useOrigSafetyDist_, minScenarioDist_);
		
		//get some fail-safe parameters to use in final stop calcs
		failSafeDistBuf_ = Double.valueOf(config.getProperty("ead.failsafe.distance.buffer"));
		failSafeResponseLag_ = Double.valueOf(config.getProperty("ead.failsafe.response.lag"));
		
		//get the HMI flag
		hmiMode_ = false;
		String hmiFlag = config.getProperty("ucr.enabled");
		if (!hmiFlag.equals("false")) {
			hmiMode_ = true;
		}
		
		//other init
		elapsedTime_ = 0.0;
		updateNeeded_ = true;
		firstTimeStep_ = true;
		mParam_ = 1.0;
		nParam_ = 1.0;
		deltaV_ = 0.0;
		speedError_ = 0.0;
	}
	
	@Override
	public double getTargetSpeed() {
		double goal = 0.0;
		
		//initialize historical values that couldn't be done well with info available in the constructor
		if (firstTimeStep_) {
			prevSpeed_ = curSpeed_;
			prevTarget_ = curSpeed_;
		}

		//identify which scenario we are in
		Scenario s = identifyScenario();
		
		//if this scenario is different from that in the previous time step or the accumulated speed error is too large then
		boolean bigSpeedError = Math.abs(speedError_) > maxSpeedError_;
		if (firstTimeStep_  ||  s != prevScenario_  ||  bigSpeedError) {
			//indicate that a new trajectory is needed
			updateNeeded_ = true;
			log_.debug("EADL", "Trajectory update required at top of loop.");
		}
		
		//if a new trajectory is needed then
		if (updateNeeded_) { //may have been set at bottom of previous call, so don't do this in above if block
			log_.infof("EADL", "/// time for a new trigonometrics trajectory calculation. Speed error = %.2f", speedError_);
			
			//reset trajectory shape parameters
			elapsedTime_ = 0.0;
			speedError_ = 0.0;
			
			//if in HMI mode (human controller) and there is a big speed error and we aren't too close to the stop bar then
			if (hmiMode_  &&  bigSpeedError  &&  dtsb_ > minScenarioDist_) {
				//see if a new main scenario type would work better
				enableScenarioChange();
				s = identifyScenario();
			}
		}
		
		//use the chosen trajectory to compute the ultimate target speed
		switch (s) {
		case OVERSPEED:
			accelMgr_.currentScenarioIs(s, curSpeed_, operSpeed_);
			goal = speedUpTrajectory();
			break;
		case OVERSPEED_EXT:
			accelMgr_.currentScenarioIs(s, curSpeed_, curSpeed_);
			goal = speedHoldTrajectory();
			break;
		case SLOWING:
			accelMgr_.currentScenarioIs(s, curSpeed_, operSpeed_);
			goal = slowTrajectory();
			break;
		case GRADUAL_STOP:
			accelMgr_.currentScenarioIs(s, curSpeed_, 0.0);
			goal = gradualStopTrajectory();
			break;
		case FINAL_STOP:
			accelMgr_.currentScenarioIs(s, curSpeed_, 0.0);
			goal = finalStopTrajectory();
			break;
		case DEPARTURE:
			accelMgr_.currentScenarioIs(s, curSpeed_, operSpeed_);
			goal = departureTrajectory();
			break;
		default:
			accelMgr_.currentScenarioIs(s, curSpeed_, operSpeed_);
			goal = constantTrajectory();
		}
		
		log_.debugf("EADL", "Returning target speed = %.2f. Accumulated speed error = %.2f", goal, speedError_);
		
		//update memory items for next time step
		firstTimeStep_ = false;
		elapsedTime_ += timeStep_;
		speedError_ += curSpeed_ - goal;
		prevScenario_ = s;
		prevTarget_ = goal;
		prevSpeed_ = curSpeed_;
		
		return goal;
	}

	//////////////////
	// member elements
	//////////////////
	
	@Override
	protected double computeEarlyArrival(double accel) {
		double te;
		double mTerm1 = 1.0;
		double mTerm2 = 1.0;
		double m = 1.0;
		double pi2m = 0.5*Math.PI;
		if (Math.abs(speedLimit_ - curSpeed_) > SPEED_TOL) {
			mTerm1 = 2.0*accel/(speedLimit_ - curSpeed_);
			mTerm2 = Math.sqrt(2.0*maxJerk_/(speedLimit_ - curSpeed_));
			m = Math.min(mTerm1, mTerm2);
			pi2m = 0.5*Math.PI/m;
			te = (dtsb_ - curSpeed_*pi2m)/speedLimit_ + pi2m;
		}else {
			te = dtsb_ / curSpeed_;
		}

		return te;
	}
	
	@Override
	protected double computeLateArrival(double accel) {
		double tl;
		double mTerm1 = 1.0;
		double mTerm2 = 1.0;
		double m = 1.0;
		double pi2m = 0.5*Math.PI;
		double minSpeed = crawlingSpeed_ + SPEED_TOL;
		if (Math.abs(curSpeed_ - minSpeed) > SPEED_TOL) {
			mTerm1 = 2.0*accel/(curSpeed_ - minSpeed);
			mTerm2 = Math.sqrt(2.0*maxJerk_/(curSpeed_ - minSpeed));
			m = Math.min(mTerm1, mTerm2);
			pi2m = 0.5*Math.PI/m;
			tl = (dtsb_ - curSpeed_*pi2m)/minSpeed + pi2m;
		}else {
			tl = dtsb_ / curSpeed_;
			log_.debug("EAD", "computeLateArrival - returning cruise time because denominator would've been too small.");
		}
		return tl;
	}

	/**
	 * always : target speed, m/s, based on the departure scenario
	 */
	private double departureTrajectory() {
		updateNeeded_ = false;
		
		//Note: if we implement a trigonometric algorithm here be sure to account for stop position offset
		//      since the vehicle will no come to a stop exactly at the stop bar (may be at positive DTSB).
		
		return operSpeed_;
	}
	
	/**
	 * always : target speed, m/s, based on the constant trajectory (white paper scenario 1)
	 */
	private double constantTrajectory() {
		updateNeeded_ = false;
		return operSpeed_;
	}
	
	/**
	 * always : target speed, m/s, is the current actual speed 
	 * @return
	 */
	private double speedHoldTrajectory() {
		updateNeeded_ = false;
		return prevTarget_;
	}

	/**
	 * always : target speed, m/s, based on the speed-up trajectory (white paper scenario 2)
	 */
	private double speedUpTrajectory() {
		double target = uniformSpeed_; //default in case below tests don't pass
		
		//compute the safety distance (this step added per updated flow chart 20150215)
		double ds = computeSafetyDistance(Scenario.OVERSPEED);
		//compute time required at previous target speed and determine if signal will be green then
		double targetTime = dtsb_/prevTarget_;
		log_.debugf("EADL", "speedUpTrajectory: ds = %.3f, targetTime = %.3f, uniformSpeed = %.3f", 
				ds, targetTime, uniformSpeed_);

		//if DTSB > safety distance or previous time step's target got us to a green signal then
		if (dtsb_ > ds  ||  getPhaseAt(targetTime).equals(SignalPhase.GREEN)) {
		
			//-----use the "normal" trigonometric eco-approach algorithm 
			
			//if we need to compute a new trajectory then
			if (updateNeeded_) {
				
				//find the ideal uniform speed, vh
				uniformSpeed_ = uniformSpeedFast();
				target = uniformSpeed_;
				log_.infof("EADL", "speedUpTrajectory - update needed. New uniformSpeed = %.2f", uniformSpeed_);
		
				//if vh is sufficiently different from current speed then
				if (Math.abs(uniformSpeed_ - curSpeed_) > SPEED_TOL) {
					try {
						//compute parameters m & n, accounting for the four defined limitations
						MNSolver solver = new MNSolver();
						solver.solve(uniformSpeed_, AccelerationManager.getManager().getAccelLimit());
						double m = solver.m();
						double n = solver.n();
						log_.debugf("EADL", "speedUpTrajectory: new m = %.4f, n = %.4f", m, n);
						
						//if we've gotten this far, the values are good, so save them for future time steps
						mParam_ = m;
						nParam_ = n;
						computeTimeBoundaries(dtsb_/uniformSpeed_);

						//compute the dv parameter for this trajectory
						deltaV_ = uniformSpeed_ - curSpeed_;

						//update completed - don't see a need for another one
						updateNeeded_ = false;
						
					} catch (Exception e) {
						//we can't compute a reasonable answer, but we know we need to speed up so just use speed limit
						// rely on the acceleration limiter to produce a reasonable straight-line trajectory
						target = speedLimit_;
						log_.warn("EADL", "Couldn't solve speedUpTrajectory, so aiming for speed limit.");
						
						//indicate that a new trajectory will need to be calculated next time around
						updateNeeded_ = true;
					}
					
				}else {
					log_.debugf("EADL", "Couldn't update m & n because curSpeed = %.2f too close to uniformSpeed = %.2f", curSpeed_, uniformSpeed_);
				}
			}
	
			try {
				//get the target speed
				target = computeTrajectory(dtsb_/uniformSpeed_);
				log_.debugf("EADL", "speedUpTrajectory - normal return from computeTrajectory.");
			} catch (Exception e) {
				//log the error, then use the default value already calculated
				log_.warn("EADL", "speedUpTrajectory could not calculate a trajectory. Using uniform speed for now.");
				
				//indicate that a new trajectory will need to be calculated next time around
				updateNeeded_ = true;
			}

		//else
		}else {
			
			//-----fall back to a constant acceleration trajectory

			//if current speed is close to operating speed and it will get us through a green light then
			if (Math.abs(curSpeed_ - operSpeed_) < SPEED_TOL  &&  getPhaseAt(dtsb_/operSpeed_).equals(SignalPhase.GREEN)) {
				//set target to operating speed
				target = operSpeed_;
			//else
			}else {
				//head for the max allowable speed
				target = speedLimit_;
			}
			
			log_.info("EADL", "speedUpTrajectory - reverting to linear trajectory");
			
			//indicate that trajectory update is needed for next time step
			updateNeeded_ = true;
		}
			
		log_.debugf("EADL", "speedUpTrajectory returning %.3f", target);
		
		return target;
	}

	/**
	 * always : target speed, m/s, based on the slow-down trajectory (white paper scenario 4)
	 */
	private double slowTrajectory() {
		double target = uniformSpeed_; //default in case below tests don't pass

		//compute the safety distance (this step added per updated flow chart 20150215)
		double ds = computeSafetyDistance(Scenario.SLOWING);
		//compute time required at previous target speed and determine if signal will be green then
		double targetTime = dtsb_/prevTarget_;
		log_.debugf("EADL", "slowTrajectory: ds = %.3f, targetTime = %.3f, uniformSpeed = %.3f", 
					ds, targetTime, uniformSpeed_);
		//if DTSB > safety distance or previous time step's target got us to a green signal then
		if (dtsb_ > ds  ||  getPhaseAt(targetTime).equals(SignalPhase.GREEN)) {
		
			//-----use the "normal" trigonometric eco-approach algorithm 
			
			//if we need to compute a new trajectory then
			if (updateNeeded_) {
				
				//find the ideal uniform speed, vh
				uniformSpeed_ = uniformSpeedSlow();
				target = uniformSpeed_;
				log_.infof("EADL", "slowTrajectory - update needed. New uniformSpeed = %.2f", uniformSpeed_);
				
				//if vh is sufficiently different from current speed then
				if (Math.abs(uniformSpeed_ - curSpeed_) > SPEED_TOL) {
					try {
						//compute parameters m & n, accounting for the four defined limitations
						MNSolver solver = new MNSolver();
						solver.solve(uniformSpeed_, -AccelerationManager.getManager().getAccelLimit());
						double m = solver.m();
						double n = solver.n();
						log_.debugf("EADL", "slowTrajectory: new m = %.4f, n = %.4f", m, n);
						
						//if we've gotten this far, the values are good, so save them for future time steps
						mParam_ = m;
						nParam_ = n;
						computeTimeBoundaries(dtsb_/uniformSpeed_);
						
						//compute the dv parameter for this trajectory
						deltaV_ = uniformSpeed_ - curSpeed_;

						//update completed - don't see a need for another one
						updateNeeded_ = false;
						
					}catch (Exception e) {
						//we know we need to slow down, so head for a crawling speed
						// rely on the acceleration limiter to produce a reasonable straight-line trajectory
						target = crawlingSpeed_;
						log_.warn("EADL", "Couldn't solve slowTrajectory, so aiming for crawling speed.");
						
						//indicate that a new trajectory will need to be calculated next time around
						updateNeeded_ = true;
					}
					
				}else {
					log_.debugf("EADL", "Couldn't update m & n because curSpeed = %.3f too close to uniformSpeed = %.3f", curSpeed_, uniformSpeed_);
				}
			}
	
			try {
				//get the target speed
				target = computeTrajectory(dtsb_/uniformSpeed_);
			} catch (Exception e) {
				//log the error, then use the default value already calculated
				log_.warn("EADL", "slowTrajectory could not calculate a trajectory. Using uniform speed for now.");
				
				//indicate that a new trajectory will need to be calculated next time around
				updateNeeded_ = true;
			}
			
		//else
		}else {
			
			//-----fall back to a constant deceleration trajectory
			
			//if current speed is close to operating speed and it will get us through a green light then
			if (Math.abs(curSpeed_ - operSpeed_) < SPEED_TOL  &&  getPhaseAt(dtsb_/operSpeed_).equals(SignalPhase.GREEN)) {
				//set target to operating speed
				target = operSpeed_;
			//else
			}else {
				//head for the slowest speed we control to
				target = crawlingSpeed_;
			}

			log_.info("EADL", "slowTrajectory - reverting to linear trajectory");
			
			//indicate that trajectory update is needed for next time step
			updateNeeded_ = true;
		}
			
		log_.debugf("EADL", "slowTrajectory returning %.3f", target);
		
		return target;
	}

	/**
	 * always : target speed, m/s, based on the complete stop trajectory (white paper scenario 3)
	 */
	private double gradualStopTrajectory() {
		
		//compute the scenario-specific time parameter as the time until the start of the next (not current) green phase
		double timeParam = timeNextGreen();
		
		//if we need to compute a new trajectory then
		if (updateNeeded_) {
			
			//since there is no ideal uniform speed here (we have to stop), set it to half the current speed (per Guoyuan email of 2/15/15)
			uniformSpeed_ = 0.5*curSpeed_;
			log_.debugf("EADL", "stopTrajectory - update needed. New uniformSpeed = %.2f", uniformSpeed_);
			
			//compute parameters m & n
			double dist = dtsb_;
			if (dist < NEAR_STOP_BAR) {
				dist = NEAR_STOP_BAR;
			}
			mParam_ = Math.PI*uniformSpeed_/dist;
			nParam_ = mParam_;

			//compute the dv parameter for this trajectory
			deltaV_ = uniformSpeed_ - curSpeed_;

			updateNeeded_ = false;
			try {
				computeTimeBoundaries(timeParam);
			} catch (Exception e) {
				//since something is wrong with time boundaries, we can't let the next section execute, so
				// set the target to what it was in previous time step and indicate an update is needed
				updateNeeded_ = true;
				log_.warn("EADL", "stopTrajectory caught exception in computing time boundaries. Setting target to prev step's");
				return prevTarget_;
			}
			log_.debugf("EADL", "stopTrajectory: new m = %.4f, n = %.4f", mParam_, nParam_);
		}
		
		double target;
		try {
			//get the target speed
			target = computeTrajectory(timeParam);
		} catch (Exception e) {
			//default to crawling speed or less if we're already there
			target = Math.min(crawlingSpeed_, curSpeed_);
			log_.warn("EADL", "Can't solve stopTrajectory. Heading for crawling speed or less.");
			
			//indicate that a new trajectory will need to be calculated next time around
			updateNeeded_ = true;
		}
		log_.debugf("EADL", "stopTrajectory returning %.3f", target);
		
		return target;
	}
	
	/**
	 * always : target speed, m/s, to a linear trajectory to zero speed at the stop bar
	 */
	private double finalStopTrajectory() {
		double target = 0.0;
		//TODO: for expediency this code is largely copied from the Trajectory.applyFailSafeCheck() method.
		//      When time permits, it needs to be refactored into a shared class.  Put it in EadBase and
		//		have it take max decel as a parameter, then Trajectory can use it for failsafe with different
		//		decel value (and maybe some other argument?)
		if (curSpeed_ > SPEED_TOL) {

			//compute the distance that will be covered during vehicle response lag
			double lagDistance = 0.0;
			if (curSpeed_ > 2.5) {
				lagDistance = failSafeResponseLag_ * curSpeed_;
			}

			//get maximum accel for this time step
			double maxAccel = AccelerationManager.getManager().getAccelLimit();
		
			//determine where we will be one time step in the future, going at the current speed (may be negative!)
			double futureDistance = dtsb_ - timeStep_*curSpeed_ - lagDistance - failSafeDistBuf_;
			//determine the speed we want to have at that point in time to achieve a smooth slow-down
			double tgt1 = Math.sqrt(Math.max(2.0*maxAccel*futureDistance, 0.0));

			//compute based on linear deceleration from current speed
			double requiredDecel = 0.5*curSpeed_*curSpeed_ / dtsb_;
			double tgt2 = Math.max(curSpeed_ - timeStep_*requiredDecel, 0.0);
			
			//use the lower of the two methods (speed has some noise in it so may give a higher or lower answer)
			target = Math.min(tgt1, tgt2);
			//reduce it a little further to maintain a command premium for the XGV to follow, and ensure the command continues to drop
			target = 0.95*Math.min(target, prevTarget_);
		}
		log_.debugf("EADL", "finalStopTrajectory returning %.3f", target);

		return target;
	}

	/**
	 * always : safety distance, ds, per UCR updated flow chart of 20150215
	 * 
	 * @return - distance, m
	 */
	private double computeSafetyDistance(Scenario s) {
		double vh;
		double ds;
		double accel = AccelerationManager.getManager().getAccelLimit();
		
		//find the uniform speed based on the scenario we are in
		switch (s) {
		case OVERSPEED:
			vh = uniformSpeedFast();
			break;
		case SLOWING:
			vh = uniformSpeedSlow();
			break;
		case GRADUAL_STOP:
		case FINAL_STOP:
			vh = 0.5*curSpeed_;
			break;
		default:
			vh = operSpeed_;
		}

		//compute the distance
		if (useOrigSafetyDist_) {
			try {
				//find the parameter m
				MNSolver solver = new MNSolver();
				solver.solve(vh, accel);
				double m = solver.m();
				ds = 0.5*Math.PI*curSpeed_/m;
				log_.debugf("EADL", "computeSafetyDistance: using white paper equation for m = %.4f, ds = %.3f", m, ds);

			} catch (Exception e) {
				//most likely reason for an exception is that vh is too close to current speed, which
				// is an ideal situation, so let's just return a small value to ensure that the normal
				// algorithm is invoked
				ds = 1.0;
				log_.infof("EADL", "computeSafetyDistance - caught exception: %s", e.toString());
			}

		}else {
			ds = 0.5*curSpeed_*curSpeed_/accel;
			log_.debugf("EADL", "computeSafetyDistance using emergency stop dist: ds = %.3f", ds);
		}
		
		return ds;
	}
	
	/**
	 * always : target speed, m/s, computed by white paper functions f(), g() and h() with the given parameters
	 * 
	 * @param timeParam - one of the terms of the equations that differentiates g() from f() and h(), s
	 */
	private double computeTrajectory(double timeParam) {
		double piOver2m = PI_OVER_2/mParam_;
		double piOver2n = PI_OVER_2/nParam_;
		double target = curSpeed_; 		//default to be used in the sixth time boundary
		log_.debugf("EADL", "computeTrajectory: dv = %.3f, elapsedTime = %.3f", deltaV_, elapsedTime_);
		
		//if time is in first segment then
		if (elapsedTime_ < timeBdry1_) {
			//compute the target speed
			target = uniformSpeed_ - deltaV_*Math.cos(mParam_*elapsedTime_);
		
		//else if time is in second segment then
		}else if (elapsedTime_ < timeBdry2_) {
			//compute the target speed
			target = uniformSpeed_ - deltaV_*mParam_/nParam_*Math.cos(nParam_*(elapsedTime_ - piOver2m + piOver2n));
		
		//else if time is in third segment then
		}else if (elapsedTime_ < timeBdry3_) {
			//compute the target speed
			target = uniformSpeed_ + deltaV_*mParam_/nParam_;
		
		//else if time is in the fourth segment then
		}else if (elapsedTime_ < timeBdry4_) {
			//compute the target speed
			target = uniformSpeed_ - deltaV_*mParam_/nParam_*Math.cos(nParam_*(elapsedTime_ - timeParam + Math.PI/nParam_));
		
		//else if time is in the fifth segment then
		}else if (elapsedTime_ < timeBdry5_) {
			//compute fifth segment target speed
			target = uniformSpeed_ - deltaV_*Math.cos(mParam_*(elapsedTime_ - timeBdry5_));
		}

		return target;
	}

	/**
	 * always : computes the five time boundaries that define the various segments of the trigonometric trajectory
	 * @throws Exception 
	 */
	private void computeTimeBoundaries(double timeParam) throws Exception {
		double piOver2m = PI_OVER_2/mParam_;
		double piOver2n = PI_OVER_2/nParam_;

		timeBdry1_ = piOver2m;
		timeBdry2_ = piOver2m + piOver2n;
		timeBdry3_ = timeParam;
		timeBdry4_ = timeParam + piOver2n;
		timeBdry5_ = timeParam + piOver2m + piOver2n;
		log_.debugf("EADL", "computeTrajectory time boundaries are: %.4f, %.4f, %.4f, %.4f, %.4f" ,
					timeBdry1_, timeBdry2_, timeBdry3_, timeBdry4_, timeBdry5_);

		//verify that the five time boundaries are properly sequenced
		if (timeBdry1_ > timeBdry2_  ||
			timeBdry2_ > timeBdry3_  ||
			timeBdry3_ > timeBdry4_  ||
			timeBdry4_ > timeBdry5_) {
			log_.warnf("EADL", "computeTimeBoundaries: time boudary sequence error. timeBdry2 = %.4f, timeBdry3 = %.4f", timeBdry2_, timeBdry3_);
			throw new Exception("Time boundary sequence error in EadUcrJava.computeTrajectory");
		}
	}

	/**
	 * time needed to cross during green > 2s : speed required to pass through intersection after the red signal expires, m/s
	 * time needed to cross during green <= 2s : current speed
	 */
	private double uniformSpeedFast() {
		double time = 0.0;

		//if current signal is green then
		if (phase_.equals(SignalPhase.GREEN)) {
			//use the time remaining in current phase
			time = timeNext_;
			
		//else if signal is yellow then
		}else if (phase_.equals(SignalPhase.YELLOW)) {
			//get the time till next green begins
			time = timeThird_;
			//sum with the full green duration
			time += greenDuration_;
			
		//else (red)
		}else {
			//get the time till green completes
			time = timeThird_;
		}
		
		//compute the speed allowing for a time buffer
		double speed = curSpeed_;
		if (time > Math.max(2.0, timeBuffer_ + 0.5)) {
			speed = dtsb_ / (time - timeBuffer_);
			if (speed < SPEED_TOL) {
				speed = SPEED_TOL;
			}else if (speed > speedLimit_) {
				speed = speedLimit_;
			}
		}
		
		return speed;
	}

	/**
	 * time needed to cross during green > 2s : speed required to pass through intersection after the red signal expires, m/s
	 * time needed to cross during green <= 2s : current speed
	 * 
	 * Assumes timeBuffer_ >= 0
	 */
	private double uniformSpeedSlow() {

		//get time till beginning of the next green cycle
		double time = timeNextGreen();
		
		//compute the speed allowing for a time buffer
		double speed = curSpeed_;
		if (time > 2.0) {
			speed = dtsb_ / (time + timeBuffer_);
			if (speed < SPEED_TOL) {
				speed = SPEED_TOL;
			}else if (speed > speedLimit_) {
				speed = speedLimit_;
			}
		}
		
		return speed;
	}
	
	private double				minScenarioDist_;	//min DTSB at which scenarios will be calculated (after this use the same one until departure), m
	private double				prevTarget_;		//target speed computed for the previous time step, m/s
	private double				elapsedTime_;		//time since the trajectory was last calculated, sec
	private double				mParam_;			//the 'm' trigonometric parameter in the white paper
	private double				nParam_;			//the 'n' trigonometric parameter in the white paper
	private double				deltaV_;			//speed difference used as a constant for a given trajectory, m/s
	private double				timeBdry1_;			//one of the 5 time boundaries that divide sections of the trigonometric trajectory, sec
	private double				timeBdry2_;			//one of the 5 time boundaries that divide sections of the trigonometric trajectory, sec
	private double				timeBdry3_;			//one of the 5 time boundaries that divide sections of the trigonometric trajectory, sec
	private double				timeBdry4_;			//one of the 5 time boundaries that divide sections of the trigonometric trajectory, sec
	private double				timeBdry5_;			//one of the 5 time boundaries that divide sections of the trigonometric trajectory, sec
	private double				uniformSpeed_;		//the hypothetical constant speed required to get from current situation through next green light, m/s
	private double				speedError_;		//accumulated error in target vs actual speed, m/s
	private double				maxSpeedError_;		//max error tolerance before a trajectory has to be recomputed, m/s
	private boolean				updateNeeded_;		//does the trajectory need to be recomputed?
	private boolean				firstTimeStep_;		//is this the first time step of the run?
	private boolean				useOrigSafetyDist_;	//should we calculate safety distance using the original (white paper) equation?
	private double				failSafeDistBuf_;	//distance buffer that we are trying to stop in front of the stop bar, m
	private double				failSafeResponseLag_; //time that the vehicle response lags the command input, sec
	private boolean				hmiMode_;			//are we operating in UCR HMI mode?
	
	private static final double	PI_OVER_2 = Math.PI/2.0;
	private static Logger		log_ = (Logger)LoggerManager.getLogger(EadUcrJava.class);
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Internal class for finding the iterative solution for the parameters m & n.  Since they have to be
	 * solved simultaneously, I don't want member variables of the main class hanging around, then we are
	 * never sure that their values are correct and self-consistent.
	 */
	private class MNSolver {
		
		public MNSolver() {
			//initialize to invalid values (valid values are always positive)
			m_ = -1.0;
			n_ = -1.0;
		}
		
		public double m() throws Exception {
			if (m_ <= 0.0) {
				throw new Exception("MNSolver: attempting to retrieve m before it is calculated.");
			}
			return m_;
		}
		
		public double n() throws Exception{
			if (n_ <= 0.0) {
				throw new Exception("MNSolver: attempting to retrieve n before it is calculated.");
			}
			return n_;
		}
		
		/**
		 * Attempts to find a solution for the inter-related trigonometric parameters m & n from the white paper.
		 * This version of the code implements the second updated flow chart, dated 20150215.
		 * 
		 * CAUTION: if input vh is too close to curSpeed_ then an exception will be thrown
		 * 
		 * @param vh - hypothetical uniform speed that would achieve traveling through a green signal, m/s
		 * @param aLimit - max accel (if positive) or decel (if negative) allowed by the system, m/s^2
		 * @throws Exception
		 */
		public void solve(double vh, double aLimit) throws Exception {
			if (vh <= 0.0  ||  Math.abs(vh - curSpeed_) <= 0.1) {
				log_.warnf("EADL", "solve: called with invalid vh = %.4f while curSpeed = %.4f", vh, curSpeed_);
				throw new Exception("MNSolver.solve: invalid vh input");
			}
			double m = 1.0;
			
			//solve the first constraint on n
			double n1 = Math.abs(aLimit / (vh - curSpeed_));
			
			//solve the second constraint on n
			double jTerm = maxJerk_ / Math.abs(vh - curSpeed_);
			double n2 = Math.sqrt(jTerm);
			
			//set n to the min of the two above constraints
			double n = Math.min(n1,  n2);
			
			//solve the third constraint on n
			double unifTime = dtsb_/vh;
			double n3 = (PI_OVER_2 - 1) / unifTime;
			
			//if our candidate n >= n3 then
			if (n >= n3) {
				//we have a valid n; compute m
				double bigTerm = PI_OVER_2 - 1 - unifTime*n;
				m = -n * (PI_OVER_2 + Math.sqrt(PI_OVER_2*PI_OVER_2 - 4.0*bigTerm)) / 2.0 / bigTerm;
				
			//else
			}else {
				log_.warnf("EADL", "Unable to solve for m & n; throwing exception. vh = %.2f, n1 = %.4f, n2 = %.4f, n3 = %.4f",
							vh, n1, n2, n3);
				//throw an exception
				throw new Exception("Unable to solve for trigonometric parameters m & n");
			}
			
			//if the results are realistic make them available for public consumption
			if (m <= 0.0  ||  n < m) {
				throw new Exception("MNSolver.solve: resultant m or n is invalid.");
			}
			m_ = m;
			n_ = n;
		}
		
		///// members
		
		private double					m_;
		private double					n_;
		
	} //end internal class MNSolver
}
