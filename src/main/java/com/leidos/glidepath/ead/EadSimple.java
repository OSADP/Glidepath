package com.leidos.glidepath.ead;

import com.leidos.glidepath.appcommon.SignalPhase;
import com.leidos.glidepath.logger.Logger;
import com.leidos.glidepath.logger.LoggerManager;

// Provides a dirt-simple algorithm to get through the intersection in the four identified scenarios
//	1 - constant speed
//	2 - speed up a little to get through before green expires
//	3 - complete stop at intersection because a red light can't be avoided
//	4 - slow down a little to give red light time to expire
//
// Motivation is to have a backup algorithm until the UCR implementation comes online.

public class EadSimple extends EadBase implements IEad {
	
	public EadSimple() {
		super();
		log_.info("EADL", "////////// Instatiating EadSimple object. //////////");

		firstTimeStep_ = true;
	}
	
	@Override
	public double getTargetSpeed() {
		double goal = 0.0;
		
		//initialize historical values that couldn't be done well with info available in the constructor
		if (firstTimeStep_) {
			prevSpeed_ = curSpeed_;
		}

		//NOTE: determining when to transition to departure mode (and then never look back). 
		// Case 1 - crossing stop bar, signal is green : change when dtsb < 0
		// Case 2 - crossing at > crawl speed, signal is yellow (timing error) : change when dtsb < 0
		// Case 3 - crossing at > crawl speed, signal is red (should never happen) : target = 0, change when signal = green
		// Case 4 - <= crawl speed in vicinity of stop bar (+/-), signal is yellow or red 
		//			(position and/or timing error) : target = 0, change when signal = green

		//if we have already transitioned into departure mode then
		if (departureLatch_) {
			//set target speed to the operating speed
			goal = operSpeed_;
			log_.debugf("EADL", "Departure trajectory - goal = %.2f", goal);
		
		//else if we are in the vicinity of the stop bar then (vehicle must be going 2*NEAR_STOP_BAR/timeStep to skip over this test)
		}else if (Math.abs(dtsb_) < NEAR_STOP_BAR) {
			//if signal is green  (case 1) then
			if (phase_ == SignalPhase.GREEN) {
				//indicate departure
				goal = operSpeed_;
				departureLatch_ = true;
				
			//else if signal is yellow and we have crossed it and speed > crawl (case 2) then
			}else if (phase_ == SignalPhase.YELLOW  &&  dtsb_ < 0.0  &&  curSpeed_ > crawlingSpeed_) {
				//indicate departure
				goal = operSpeed_;
				departureLatch_ = true;
				
			//else (case 3 & 4)
			}else {
				//stop the vehicle
				goal = 0.0;
			}
			log_.debugf("EADL", "In vicinity of stop bar - goal = %.2f, departure transition = %b", goal, departureLatch_);
		
		//else (one of the approach scenarios)
		}else {
		
			//compute cruise time (time to reach stop bar at operating speed)
			double timeRem = dtsb_ / operSpeed_;
			//determine what phase the signal will be in at that time
			SignalPhase cruisePhase = getPhaseAt(timeRem);
			
			//if the signal will be green then
			if (cruisePhase == SignalPhase.GREEN) {
				//target is the operating speed
				goal = operSpeed_;
				log_.debugf("EADL", "Cruise trajectory - goal = %.2f", goal);
				
			//else (we will need some sort of speed adjustment)
			}else {
			
				//NOTE:  we only know about 3 signal phases, the current one and the next two. We only know the durations for
				// the current and next phases. Therefore, we will treat the third phase as infinitely long.
				// Given this model of the world, scenario 2 is only possible if the current phase is green or red.  And scenario
				// 4 only makes sense if the current phase is yellow or red.
				
				//compute a speed-up trajectory that reaches the stop bar before the green expires
				goal = findSpeedUpTarget();
				log_.debugf("EADL", "1. Speed up trajectory gives goal = %.2f", goal);
				
				//if a speed-up trajectory is not possible then (we will need to slow down)
				if (goal <= 0.0) {
	
					//compute latest time of arrival without stopping (stay above crawling speed)
					goal = findSlowDownTarget();
					log_.debugf("EADL", "2. Slow-down trajectory gives goal = %.2f", goal);
					
					//if a slow-down trajectory is not possible then
					if (goal <= 0.0) {
						
						//set trajectory for a complete stop with gradual deceleration to the stop bar
						double decel = 0.5*curSpeed_*curSpeed_/dtsb_; //convention is for decel to be positive
						goal = curSpeed_ - decel*timeStep_;
						log_.debugf("EADL", "3. Need to stop. Current goal = %.2f", goal);
					}
				}
			} //endif (need a speed adjustment)
		} //endif (approach)
		
		log_.debugf("EADL", "Returning target speed = %.2f", goal);
		
		//update history for the next time step
		firstTimeStep_ = false;
		prevSpeed_ = curSpeed_;
		
		return goal;
	}
	
	
    ////////////////////
    // protected members
    ////////////////////
    
	@Override
	protected double computeEarlyArrival(double accel) {
		return 0.0; //irrelevant to this model
	}

	@Override
	protected double computeLateArrival(double accel) {
		return 0.0; //irrelevant to this model
	}


    //////////////////
    // private members
    //////////////////
    
	private double findSpeedUpTarget() {
		double ts = 0.0;
		double endTime = 0.0;
		
		//if current signal phase is green then
		if (phase_ == SignalPhase.GREEN) {
			//end time for trajectory is remaining phase time - safety margin
			endTime = timeNext_ - timeBuffer_;
		//else if current phase is red then
		}else if (phase_ == SignalPhase.RED) {
			//end time for trajectory is time till third phase - safety margin
			endTime = timeThird_ - timeBuffer_;
		//else
		}else {
			//no solution possible - return
			return 0.0;
		}
		
		//get maximum accel for this time step
		double maxAccel = AccelerationManager.getManager().getAccelLimit();
	
		//compute target speed necessary to reach stop bar in the time remaining, assuming constant acceleration.
		// At the max allowed acceleration rate.  There are two parts to this trajectory. The first is from the current distance, Dc,
		// to the transition point, Dt, and is constant acceleration. At Dt our speed will be the target speed. The second part is
		// from Dt to the stop bar at D=0 (D = distance remaining to stop bar), and has a constant speed.  The time to go from
		// Dt to 0 is Dt/S, where S is the target speed.  The time to go from current position to Dt is given by 
		// (Dc - Dt) = a*t^2/2, or t = sqrt(2*(Dc - Dt)/a).  The sum of these two times gives us the constraint to be satisfied:
		// Dt/S + sqrt(2*(Dc - Dt)/a) <= Trem, where Trem is the time remaining before green expires.
		// Or, S >= Dt/(Trem - sqrt(2*(Dc - Dt)/a).  And the time it takes to go from current speed to the target speed is
		// (S - S0)/a.  Combining these four equations and simplifying the resulting quadratic formula, we get
		// S >= a*Trem +/- sqrt(a^2*Trem^2 - 2a*Dc + S0^2).  If Trem and S0 are large enough and Dc is small enough, then
		// there are valid roots, but only the positive one makes sense physically since S needs to be larger than S0.
		double atr = maxAccel*endTime;
		double square = atr*atr - 2.0*maxAccel*dtsb_ + curSpeed_*curSpeed_;
		if (square >= 0.0) {
			
			//use the root that is between current speed and speed limit, if one exists
			double root1 = maxAccel*endTime - Math.sqrt(square);
			double root2 = maxAccel*endTime + Math.sqrt(square);
			if (root1 > curSpeed_  &&  root1 < speedLimit_) {
				ts = root1;
			}else if (root2 > curSpeed_  &&  root2 < speedLimit_) {
				ts = root2;
			}
		}
		
		return ts;
	}
	
	private double findSlowDownTarget() {
		double ts = 0.0;
		double endTime = 0.0;
		
		//if current signal phase is red then
		if (phase_ == SignalPhase.RED) {
			//end time for trajectory is remaining phase time + safety margin
			endTime = timeNext_ + timeBuffer_;
		//else if current phase is yellow then
		}else if (phase_ == SignalPhase.YELLOW) {
			//end time for trajectory is time till third phase + safety margin
			endTime = timeThird_ + timeBuffer_;
		//else
		}else {
			//no solution possible - return
			return 0.0;
		}
		
		//get maximum accel for this time step
		double maxDecel = AccelerationManager.getManager().getAccelLimit(); //a positive value
	
		//compute target speed necessary to reach stop bar after the red phase expires, assuming constant deceleration.
		// At the max allowed deceleration rate.  There are two parts to this trajectory. The first is from the current distance, Dc,
		// to the transition point, Dt, and is constant deceleration. At Dt our speed will be the target speed. The second part is
		// from Dt to the stop bar at D=0 (D = distance remaining to stop bar), and has a constant speed.  The time to go from
		// Dt to 0 is Dt/S, where S is the target speed.  The time to go from current position to Dt is given by 
		// (Dc - Dt) = a*t^2/2, or t = sqrt(2*(Dc - Dt)/a).  The sum of these two times gives us the constraint to be satisfied:
		// Dt/S + sqrt(2*(Dc - Dt)/a) >= Trem, where Trem is the time remaining until red expires.
		// Or, S <= Dt/(Trem - sqrt(2*(Dc - Dt)/a).  And the time it takes to go from current speed to the target speed is
		// (S - S0)/a.  Combining these four equations and simplifying the resulting quadratic formula, we get
		// S <= a*Trem +/- sqrt(a^2*Trem^2 - 2a*Dc + S0^2).  Since a is always negative in this deceleration scenario, there will  
		// always be two roots, but we want the one that makes S smaller than S0.
		double atr = -maxDecel*endTime;
		double root = Math.sqrt(atr*atr + 2.0*maxDecel*dtsb_ + curSpeed_*curSpeed_);
		double root1 = -maxDecel*endTime + root;
		double root2 = -maxDecel*endTime - root;
		if (root1 < curSpeed_  &&  root1 > crawlingSpeed_) {
			ts = root1;
		}else if (root2 < curSpeed_  &&  root2 > crawlingSpeed_) {
			ts = root2;
		}
		
		return ts;
	}

	private boolean				firstTimeStep_;			//is this the first time step of the solution?
	private static Logger		log_ = (Logger)LoggerManager.getLogger(EadSimple.class);
	
}
