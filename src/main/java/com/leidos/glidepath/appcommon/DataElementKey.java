package com.leidos.glidepath.appcommon;


public enum DataElementKey {
    TIME_SINCE_FIRST_MOTION,		// elapsed time since first motion was detected, sec
    OPERATING_SPEED,				// user-defined desired cruise speed, m/s
    SPEED_COMMAND,					// command to be sent to the XGV, m/s
    SMOOTHED_SPEED,                 // computed speed via filtering based on configured filter
    SPEED,							// actual current vehicle speed, m/s
    ACCELERATION,                   // acceleration in m/s squared
    JERK,                           // rate of acceleration  m/s -3
    LATITUDE,						// deg
    LONGITUDE,						// deg
    DIST_TO_STOP_BAR,				// m (double)
    SIGNAL_PHASE,					// phase (color) of the traffic signal
    SIGNAL_TIME_TO_NEXT_PHASE,		// double
    SIGNAL_TIME_TO_THIRD_PHASE,		// double
    MOTION_STATUS,                  // MotionStatus element
    LANE_ID,						// ID of the lane that vehicle is traveling in
    CYCLE_GPS,                      // length of consumer call() method in ms....int
    CYCLE_MAP,
    CYCLE_SPAT,
    CYCLE_XGV,
    CYCLE_EAD,
    CYCLE_XGV_COMMAND,
    XGV_STATUS,                     // XgvStatus ojbect
    MAP_MESSAGE,					// MAP message content from the ASD
    SPAT_MESSAGE,                   // SPAT message content
    STATUS_MESSAGE                  // StringBuffer element containing status messages
}
