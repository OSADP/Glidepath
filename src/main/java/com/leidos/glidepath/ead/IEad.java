package com.leidos.glidepath.ead;

// Interface to any of the possible EAD algorithm classes that we may choose to use.  Defined methods are limited to
// simple numeric parameters with no exceptions because of the need to accommodate a JNI interface to C implementation.

public interface IEad {

	public void initialize(long timestep, int logFile);
	
	public void setState(double speed, double operSpeed, double accel, double distance, int phase, double timeNext, double timeThird);
	
	public double getTargetSpeed();
	
	public void setStopBoxWidth(double width);
	
}
