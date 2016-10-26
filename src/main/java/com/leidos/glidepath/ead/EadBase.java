package com.leidos.glidepath.ead;

import com.leidos.glidepath.appcommon.Constants;
import com.leidos.glidepath.appcommon.SignalPhase;
import com.leidos.glidepath.logger.Logger;
import com.leidos.glidepath.logger.LoggerManager;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;

public abstract class EadBase implements IEad {

	public EadBase() {
		departureLatch_ = false;
		
		//get signal phase durations, since current SPAT technology does not provide info on the full cycle
		AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
		greenDuration_	= Double.valueOf(config.getProperty("spat.green"));
		yellowDuration_ = Double.valueOf(config.getProperty("spat.yellow"));
		redDuration_	= Double.valueOf(config.getProperty("spat.red"));
		
		//get time buffer to be applied to start/end of future phases
		timeBuffer_		= Double.valueOf(config.getProperty("ead.timebuffer"));
		
		//max jerk and speed limit
		maxJerk_		= Double.valueOf(config.getProperty("maximumJerk"));
		speedLimit_		= Double.valueOf(config.getProperty("maximumSpeed"))/Constants.MPS_TO_MPH;
		
		//configure crawling speed
		crawlingSpeed_ 	= Double.valueOf(config.getProperty("crawlingSpeed")) / Constants.MPS_TO_MPH;
		log_.infof("EADL", "time buffer = %.2f, crawlingSpeed = %.2f", timeBuffer_, crawlingSpeed_);
		
		//get access to the acceleration manager
		accelMgr_ = AccelerationManager.getManager();
		
		//initialize the previous time step's speed to something in the mid-range, but it will be overridden in child classes
		prevSpeed_ = 10.0;
		
		//driver control of the test run
		departureAuthorized_ = false;
		
		//other init
		prevScenario_ = Scenario.RAMP_UP;
		numZeroSpeeds_ = 0;
		reevaluateScenario_ = false;

		//other member variables are initialized by one of the below methods, so not needed here
	}

	@Override
	public void initialize(long timestep, int logFile) {
		timeStep_ = (double)timestep / 1000.0;
	}

	@Override
	public void setState(double speed, double operSpeed, double accel, double distance, int phase,
			double timeNext, double timeThird) {
		curSpeed_ = speed;
		operSpeed_ = operSpeed;
		curAccel_ = accel;
		dtsb_ = distance;
		timeNext_ = timeNext;
		timeThird_ = timeThird;
		switch (phase) {
		case 0:
			phase_ = SignalPhase.GREEN;
			break;
		case 1:
			phase_ = SignalPhase.YELLOW;
			break;
		default:
			phase_ = SignalPhase.RED;	//throwing an exception is not allowed, so this is the best we can do
		}
	}
	
	/**
	 * always : target speed that we want the vehicle to travel, m/s
	 */
	@Override
	public abstract double getTargetSpeed();
	
	@Override
	public void setStopBoxWidth(double width) {
		stopBoxWidth_ = width;
	}

	////////////////////
	// protected members
	////////////////////
	
	/**
	 * always : indicates that we need to re-evaluate a main scenario change
	 */
	protected void enableScenarioChange() {
		reevaluateScenario_ = true;
	}

	/**
	 * Classifies the current trajectory scenario into one of five categories, based on current speed, operating speed,
	 * remaining distance to stop bar, and where we are in the signal's cycle timing.  This is a state machine that locks
	 * the vehicle into one of the four primary scenarios (states) from initial decision point until we reach the
	 * intersection.*  If it turns out that the vehicle can't achieve the plan and needs to make a stop, that will be
	 * handled by the Trajectory class's fail-safe logic, which will override any command produced by this class.
	 * But it won't change our state here.
	 * 
	 * *Okay, this concept has been bastardized a bit to accommodate HMI testing, which needs the ability to switch
	 * from one scenario to another throughout the route because of the uncertainty in human driver performance. This
	 * is the sole purpose of the reevaluateScenario_ flag.
	 * 
	 * @return scenario defined in the enum Scenario
	 */
	protected Scenario identifyScenario() {
		Scenario s = prevScenario_;
		
		//if we are just coming off a ramp-up maneuver (this state only exists for one time step) or
		// we have been told to reconsider which main scenario to use then
		if (s.equals(Scenario.RAMP_UP)  ||  reevaluateScenario_) {
			//pick the main scenario that will dictate the high-level type of trajectory to use
			s = pickMainScenario();
			
			//don't allow this to happen again unless explicitly commanded
			reevaluateScenario_ = false;
			
		}else {

			//do the work each state requires 
			switch(s) {
			case CONSTANT:
				s = constantState();
				break;
				
			case OVERSPEED:
				s = overspeedState();
				break;
				
			case GRADUAL_STOP:
				s = gradualStopState();
				break;
				
			case SLOWING:
				s = slowingState();
				break;
				
			case OVERSPEED_EXT:
				s = overspeedExtendedState();
				break;
				
			case FINAL_STOP:
				s = finalStopState();
				break;
				
			case DEPARTURE:
				s = departureState();
				break;
				
			default:
				log_.errorf("EADL", "identifyScenario: unknown scenario %S", s.toString());
			}
		}
		
		if (s != prevScenario_) {
			log_.infof("EADL", "/// Trajectory scenario changed from %s to %s", prevScenario_.toString(), s.toString());
		}
		
		return s;
	}

	/**
	 * To be overridden by derived class.  
	 * always : time to stop bar if vehicle accelerates to the speed limit, sec
	 * 
	 * @accel - maximum allowable acceleration, m/s^2
	 */
	protected abstract double computeEarlyArrival(double accel);
	
	
	/**
	 * To be overridden by derived class.
	 * always : time to stop bar if vehicle slows to the crawling speed, sec
	 * 
	 * @accel - maximum allowable acceleration, m/s^2
	 */
	protected abstract double computeLateArrival(double accel);
	
	
	/**
	 * always : signal phase that will be active at futureTime seconds in the future
	 */
	protected SignalPhase getPhaseAt(double futureTime) {
		if (futureTime <= timeNext_) {
			return phase_;
		}else if (futureTime <= timeThird_) {
			return phase_.next();
		}else {
			double thirdDuration;
			SignalPhase phase3 = phase_.next().next();
			SignalPhase phase4 = phase3.next();
			
			switch(phase3) {
			case GREEN:
				thirdDuration = greenDuration_;
				break;
			case YELLOW:
				thirdDuration = yellowDuration_;
				break;
			default:
				thirdDuration = redDuration_;
			}
			
			if (futureTime < timeThird_ + thirdDuration) {
				return phase3;
			}else {
				return phase4; //assumes it won't go past 4 phases
			}
		}
	}

	/**
	 * current phase == GREEN : time to beginning of following green phase
	 * current phase != GREEN : time to beginning of next green phase
	 * 
	 * @return time in sec
	 */
	protected double timeNextGreen() {
		double time = 0.0;
		
		if (phase_.equals(SignalPhase.GREEN)) {
			time = timeThird_ + redDuration_;
		}else if (phase_.equals(SignalPhase.YELLOW)) {
			time = timeThird_;
		}else {
			time = timeNext_;
		}
		
		return time;
	}

	protected Scenario				prevScenario_;			//scenario from the previous time step
	protected boolean				departureLatch_;		//have we transitioned to departure mode?
	protected boolean				departureAuthorized_;	//are we authorized to egress from the intersection (driver command)?
	protected SignalPhase			phase_;					//signal phase
	protected double				timeStep_;				//duration of one time step, sec
	protected double				maxJerk_;				//maximum allowed jerk, m/2^3
	protected double				speedLimit_;			//maximum allowed speed, m/s
	protected double				curSpeed_;				//current actual speed of the vehicle, m/s
	protected double				operSpeed_;				//the user's intended "normal" speed, m/s
	protected double				curAccel_;				//current actual acceleration, m/s^2
	protected double				dtsb_;					//distance uptrack of stop bar, m
	protected double				timeNext_;				//time to the next signal phase, sec
	protected double				timeThird_;				//time to the signal phase after next, sec
	protected double				greenDuration_;			//duration of the signal's green phase, sec
	protected double				yellowDuration_;		//duration of the signal's yellow phase, sec
	protected double				redDuration_;			//duration of the signal's red phase, sec
	protected double				timeBuffer_;			//amount of extra time to add/subtract to future phase boundary, sec
	protected double				prevSpeed_;				//vehicle's actual speed in the previous time step, m/s
	protected double				crawlingSpeed_;			//speed below which EAD will bring the vehicle to a stop
	protected AccelerationManager	accelMgr_;				//manages acceleration limits based on where we are in the trajectory
	protected int					numZeroSpeeds_;			//number of consecutive time steps with speed <= 0 after we leave the GRADUAL_STOP state
	protected double				stopBoxWidth_;			//distance from one side of stop box to the other, meters
	private boolean					reevaluateScenario_;	//should we re-evaluate the major scenario branch?
	
	protected static final double	SPEED_TOL = 0.2;		//tolerance on difference between uniform speed and current speed, m/s
	protected static final double	NEAR_STOP_BAR = 12.0;	//distance, m, from stop bar within which we need to consider ourselves at the bar
	protected static Logger			log_ = (Logger)LoggerManager.getLogger(EadBase.class);
	
	//////////////////
	// private members
	//////////////////
	

	/**
	 * Logic to choose one of the four main trajectory types going through the intersection.
	 */
	private Scenario pickMainScenario() {
		Scenario s = Scenario.CONSTANT;
		
		//get maximum accel for this time step
		double maxAccel = AccelerationManager.getManager().getAccelLimit();
	
		//compute cruise time (time to reach stop bar at operating speed)
		double tc = dtsb_/operSpeed_;
		
		//determine what phase the signal will be in at that time
		SignalPhase phaseAtCrossing = getPhaseAt(tc);
		
		//if it will not be green then
		if (phaseAtCrossing != SignalPhase.GREEN) {
			
			//get earliest possible time of arrival
			double te = 999.9;
			te = computeEarlyArrival(maxAccel);
			
			//determine what phase the signal will be in at that time
			phaseAtCrossing = getPhaseAt(te + timeBuffer_);
			log_.debugf("EADL", "pickMainScenario: te = %.3f", te);
			
			//if the signal is currently green and it will be green then
			// (note: this is a kludge because it assumes a long signal cycle compared to the drive time)
			if (phase_.equals(SignalPhase.GREEN)  &&  phaseAtCrossing.equals(SignalPhase.GREEN)) {
				//choose scenario 2
				s = Scenario.OVERSPEED;
		
			//else (will need to slow down)
			}else {
				
				//if speed is sufficiently above crawling then
				if (curSpeed_ > (crawlingSpeed_ + SPEED_TOL)) {
					
					//get latest possible time of arrival
					double tl = 0.01;
					tl = computeLateArrival(maxAccel);
	
					//if the signal will turn green by then
					if (timeNextGreen() <= tl - timeBuffer_){
						//choose scenario 4 (slow drive through)
						s = Scenario.SLOWING;
						log_.debugf("EADL", "Chose slowing trajectory: tl = %.3f", tl);
						
					//else (scenario 3, complete stop, gradually)
					}else {
						s = Scenario.GRADUAL_STOP;
						log_.debugf("EADL", "Chose stopping trajectory: tl = %.3f", tl);
					}
					
				//else (stop scenario)
				}else {
					s = Scenario.GRADUAL_STOP;
				}
			}
		} //endif not green
		
		
		return s;
	}
	
	/**
	 * Handles state transition out of the CONSTANT state
	 */
	private Scenario constantState() {
		Scenario s = Scenario.CONSTANT;
		
		if (dtsb_ < 0.0  &&  (phase_.equals(SignalPhase.GREEN)  ||  phase_.equals(SignalPhase.YELLOW))) {
			s = Scenario.DEPARTURE;
		}
		
		return s;
	}
	
	/**
	 * Handles state transition out of the OVERSPEED state
	 */
	private Scenario overspeedState() {
		Scenario s = Scenario.OVERSPEED;
		
		if (dtsb_ < NEAR_STOP_BAR) {
			s = Scenario.OVERSPEED_EXT;
		}
		
		return s;
	}
	
	/**
	 * Handles state transition out of the OVERSPEED_EXT state
	 */
	private Scenario overspeedExtendedState() {
		Scenario s = Scenario.OVERSPEED_EXT;
		
		if (dtsb_ < -stopBoxWidth_) {
			s = Scenario.DEPARTURE;
		}
		
		return s;
	}
	
	/**
	 * Handles state transition out of the GRADUAL_STOP state
	 */
	private Scenario gradualStopState() {
		Scenario s = Scenario.GRADUAL_STOP;
		
		if (curSpeed_ < (crawlingSpeed_ + SPEED_TOL)  &&  dtsb_ < 1.46*NEAR_STOP_BAR) {
			s = Scenario.FINAL_STOP;
			numZeroSpeeds_ = 0;
		}
		
		return s;
	}
	
	/**
	 * Handles state transition out of the SLOWING state
	 */
	private Scenario slowingState() {
		Scenario s = Scenario.SLOWING;
		
		if (dtsb_ < 0.0  &&  phase_.equals(SignalPhase.GREEN)) {
			s = Scenario.DEPARTURE;
		}
		
		return s;
	}
	
	/**
	 * Handles state transition out of the FINAL_STOP state
	 */
	private Scenario finalStopState() {
		Scenario s = Scenario.FINAL_STOP;
		
		//make sure we have zero speed for several consecutive time steps before we claim the vehicle
		// is stopped (note that smoothed speed data may be negative)
		if (curSpeed_ <= 0.0) {
			++numZeroSpeeds_;
		}
		if (numZeroSpeeds_ > 4  &&  phase_.equals(SignalPhase.GREEN)) {
			s = Scenario.DEPARTURE;
		}
		
		return s;
	}
	
	/**
	 * Handles state transition out of the DEPARTURE state
	 */
	private Scenario departureState() {
		//for this phase of Glidepath, we stay in departure state until the app shuts down
		return Scenario.DEPARTURE;
	}
}
