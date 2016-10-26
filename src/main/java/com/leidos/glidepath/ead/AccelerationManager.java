package com.leidos.glidepath.ead;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;

/**
 * A singleton class to manage acceleration limits in the various trajectory situations.
 * 
 * @author starkj
 *
 */

public class AccelerationManager {
	
	/**
	 * Returns a reference to the one and only acceleration manager object
	 */
	public static AccelerationManager getManager() {
		return INSTANCE;
	}
	
	/**
	 * Returns the accel/decel limit that applies to the current time step, m/s^2
	 * 
	 * Value is always positive.
	 */
	public double getAccelLimit() {
		return curLimit_;
	}
	
	public void currentScenarioIs(Scenario s, double curSpeed, double desiredSpeed) {
		
		//if scenario is ramp-up or departure then
		if (s == Scenario.RAMP_UP  ||  s == Scenario.DEPARTURE) {
			//if current speed is significantly different from desired speed then
			if (Math.abs(curSpeed - desiredSpeed) > 0.5) {
				//if it is ramp-up then
				if (s == Scenario.RAMP_UP) {
					//use the ramp-up situation
					curLimit_ = rampUp_;
				//else if current speed > desired then
				}else if (curSpeed > desiredSpeed) {
					//this is a speed-up departure
					curLimit_ = speedupDeparture_;
				//else
				}else {
					//this is a slow-down departure
					curLimit_ = slowDownDeparture_;
				}
		
			//else (we're virtually where we want to be)
			}else {
				//use the default situation
				curLimit_ = defaultAccel_;
			}
		
		//else if scenario is speed-up then
		}else if (s == Scenario.OVERSPEED) {
			//use the speed-up initial situation
			curLimit_ = speedupInitiate_;
		
		//else if speed is to be held constant then
		}else if (s == Scenario.CONSTANT  ||  s == Scenario.OVERSPEED_EXT) {
			//use the default situation
			curLimit_ = defaultAccel_;
		
		//else (must be some form of slow-down or stop scenario)
		}else {
			//use the slow-down initial situation
			curLimit_ = slowDownInitiate_;
		}
	}
	
	////////////////////
	// private members
	////////////////////

	/**
	 * Constructor explicitly defined to do nothing
	 */
	private AccelerationManager() {	
		//get the various limits defined in the config file
		AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
		defaultAccel_		= Double.valueOf(config.getProperty("defaultAccel"));
		rampUp_				= Double.valueOf(config.getProperty("rampUpAccel"));
		speedupInitiate_	= Double.valueOf(config.getProperty("scenario2InitiateAccel"));
		slowDownInitiate_	= Double.valueOf(config.getProperty("scenario3InitiateAccel"));
		speedupDeparture_	= Double.valueOf(config.getProperty("scenario2DepartureAccel"));
		slowDownDeparture_	= Double.valueOf(config.getProperty("scenario3DepartureAccel"));
		
		//initialize the default acceleration in case nobody ever specifies the scenario
		curLimit_ = defaultAccel_;
	}
	
	private static final AccelerationManager INSTANCE = new AccelerationManager();	//the one and only object of this type

	private static double					curLimit_;		//the acceleration limit that applies to the current situation, m/s^2
	
	//below are the possible limits, depending on the situation we find ourselves in
	private static double					defaultAccel_;
	private static double					rampUp_;
	private static double					speedupInitiate_;
	private static double					slowDownInitiate_;
	private static double					speedupDeparture_;
	private static double					slowDownDeparture_;
}
