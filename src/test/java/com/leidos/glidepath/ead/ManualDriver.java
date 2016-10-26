package com.leidos.glidepath.ead;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.leidos.glidepath.appcommon.Constants;

public class ManualDriver {

	///////////////////////////////
	///// JNI configuration section
	///////////////////////////////

	//forward declaration of each of the C functions in the EAD library
	public native void		initialize(long ts, int lf);
	public native void		set_constraints(double ma, double md, double mj, double sl);
	public native void		set_state(double sp, double ds, double di, int ph, double t1, double t2);
	public native double	get_target_speed();
	public native void		close_ead_logs();
	
	static {
		if (!System.getProperty("os.name").toLowerCase().startsWith("win"))   {
			try {
				System.loadLibrary("EADTest");	//this is the external C library (libEAD.so).
	            							// LoadLibrary() searches the java.library.path for it. Can use load() to specify abs path.
			} catch (Exception e) {
				System.out.println("Exception trapped while trying to load the EAD library: " + e.toString());
				e.printStackTrace();
			}
		}
	}
	
	//////////////////////////////
	///// End of JNI configuration
	//////////////////////////////

	public ManualDriver() {
		timeStep_ = 100;
		curSpeed_ = 0.0;
		maxAccel_ = 3.0;
		maxDecel_ = 9.0;
		maxJerk_ = 5.0;
		operSpeed_ = 15.0/Constants.MPS_TO_MPH;
		speedLimit_ = 35.0/Constants.MPS_TO_MPH;
		distance_ = 100.0;
		phase_ = 0; //green
		time1_ = 30.0;
		time2_ = 35.0;
	}
	
	public static void main(String[] args) {
		ManualDriver md = new ManualDriver();
		md.displayMenu();
	}
	
	public void displayMenu() {
		double cmd = 0.0;
		
		//initialize the library
		System.out.println("Enter initializing data:");
		timeStep_ = (int)getVal("Time step, ms", (double)timeStep_);
		maxAccel_ = getVal("Max accel, m/s^2", maxAccel_);
		maxDecel_ = getVal("Max decel, m/s^2", maxDecel_);
		maxJerk_  = getVal("Max jerk, m/s^3", maxJerk_);
		speedLimit_ = getVal("Spd limit, m/s", speedLimit_);
		operSpeed_ = getVal("Oper speed, m/s", operSpeed_);
		int logOpt = (int)getVal("Log to 1:file, 2:stdout", 2);
		initialize(timeStep_, logOpt);
		set_constraints(maxAccel_, maxDecel_, maxJerk_, speedLimit_);

		//loop through routine inputs
		System.out.println();
		System.out.println("Ready to get speed commands.");
		do {
			curSpeed_ = getVal("Current speed, m/s", curSpeed_);
			distance_ = getVal("DTSB, m    ", distance_);
			phase_ = (int)getVal("Phase (0=G, 2=R/Y)", (double)phase_);
			time1_ = getVal("Time next phase, s", time1_);
			time2_ = getVal("Time third phase, s", time2_);
			
			set_state(curSpeed_, operSpeed_, distance_, phase_, time1_, time2_);
			cmd = get_target_speed();
			System.out.println();
			System.out.println("Command = " + cmd);
			System.out.println();
		}while (true);
	}
	
	public double getVal(String label, double prev) {
		
		System.out.print("   " + label + " (" + prev + "): ");
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
		
		String result = null;
		try {
			result = r.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double val;
		try {
			val = Double.valueOf(result);
		} catch (NumberFormatException e) {
			val = prev; //user has probably hit enter to accept the given value
		}
		
		return val;
	}
	
	//////////////////
	// member elements
	//////////////////
	
	private int				timeStep_;
	private int				phase_;
	private double			curSpeed_;
	private double			maxAccel_;
	private double			maxDecel_;
	private double			maxJerk_;
	private double			operSpeed_;
	private double			speedLimit_;
	private double			distance_;
	private double			time1_;
	private double			time2_;
}
