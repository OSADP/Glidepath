package com.leidos.glidepath.ead;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.Logger;
import com.leidos.glidepath.logger.LoggerManager;

/**
 * THIS IS AN EXPERIMENTAL CLASS ONLY, AND SHOULD NOT BE USED FOR ANY PRODUCTION GLIDEPATH TESTS.
 * 
 * Its purpose is to allow methodical evaluation of the vehicle dynamics, including the response of the
 * whole control linkage chain (XGV, throttle, engine, vehicle inertia).
 * 
 * What I would like to do with this class is, after a number of runs with different parameter values,
 * be able to set those values to accurately characterize the vehicle under a variety of circumstances,
 * such that we can then use this class to control the actual speed trajectory along a known path.
 * 
 * @author starkj
 *
 */
public class EadDynamics extends EadBase {
	
	public EadDynamics() {
		super();
		log_.info("EADL", "////////// Instatiating EadDynamics object. //////////");
		
		//get the config parameters we will need
		AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
		targetAccel_	= Double.valueOf(config.getProperty("ead.Xtargetaccel"));
		maxCmdAdj_ 	= Double.valueOf(config.getProperty("ead.maxcmdadj"));
		cmdAccelGain_	= Double.valueOf(config.getProperty("ead.cmdaccelgain"));
		cmdSpeedGain_	= Double.valueOf(config.getProperty("ead.cmdspeedgain"));
		cmdBias_	= Double.valueOf(config.getProperty("ead.cmdbias"));
		
		//other initializations
		time_ = 0.0;
		
		log_.infof("EADL", "Initializing with targetAccel = %.2f", targetAccel_);
	}

	@Override
	public double getTargetSpeed() {
		double cmd = operSpeed_;
		
		time_ += 0.1; //assume that each time step is exactly the right duration

		//get maximum accel for this time step
		double maxAccel = AccelerationManager.getManager().getAccelLimit();
	
		//limit the target acceleration based on the maxes defined (need to do this here because maxes aren't defined until initialize() is called
		if (targetAccel_ >= 0.0) {
			if (targetAccel_ > maxAccel) {
				targetAccel_ = maxAccel;
			}
		}else {
			if (targetAccel_ < -maxAccel) {
				targetAccel_ = -maxAccel;
			}
		}
		
		//compute the desired speed we want from this time step, as a uniform increment from the previous time step
		double desiredSpeed = time_*targetAccel_ + operSpeed_;
		if (desiredSpeed > speedLimit_) {
			desiredSpeed = speedLimit_;
		}else if (desiredSpeed < 0.0) {
			desiredSpeed = 0.0;
		}
		
		//get the command adjustment that will coax the XGV into achieving this speed
		double premium = calcControlAdjustment(curSpeed_, desiredSpeed);
		
		//compute the command
		cmd = desiredSpeed + premium;
		log_.infof("EADL", "Changing speeds - desiredSpeed = %.4f, premium = %.4f, cmd = %.4f", desiredSpeed, premium, cmd);

		return cmd;
	}

	//////////////////
	// member elements
	//////////////////

	@Override
	protected double computeEarlyArrival(double accel) {
		return 0.0; //irrelevant to this model
	}

	@Override
	protected double computeLateArrival(double accel) {
		return 0.0; //irrelevant to this model
	}


	private double calcControlAdjustment(double actSpeed, double desiredSpeed) {
		double p = 0.0;
		double sign = 1.0;
		
		if (targetAccel_ < 0.0) {
			sign = -1.0;
		}
		
		//calculate the adjustment
		p = cmdAccelGain_*(targetAccel_ - curAccel_) + cmdSpeedGain_*(desiredSpeed - actSpeed) + sign*cmdBias_;
	
		if (p > maxCmdAdj_) {
			p = maxCmdAdj_;
		}else if (p < -maxCmdAdj_) {
			p = -maxCmdAdj_;
		}
		
		return p;
	}
	
	private double cmdAccelGain_;	
	private double cmdSpeedGain_;
	private double cmdBias_;
	private double maxCmdAdj_;
	private double time_;				//time since we started the trajectory, sec
	private double targetAccel_;		//the constant acceleration behavior that we'd like to test (positive or negative)
	
	private static Logger		log_ = (Logger)LoggerManager.getLogger(EadDynamics.class);
}
