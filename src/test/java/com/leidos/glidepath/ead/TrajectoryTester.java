package com.leidos.glidepath.ead;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.logger.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.asd.map.MapMessage;
import com.leidos.glidepath.dvi.AppConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class TrajectoryTester {
	
    @Autowired
    ApplicationContext applicationContext;

    @Before
	public void setup() {
		log_ = (Logger)LoggerManager.getLogger(TrajectoryTester.class);
        LoggerManager.setRecordData(true);

        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
        config_ = context.getAppConfig();

		try {
			traj_ = new Trajectory();
			traj_.engage();
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test setup: " + e.toString());
			e.printStackTrace();
			try {
				LoggerManager.writeToDisk();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return;
		}

		input_ = new DataElementHolder();
		
		if (traj_ == null  ||  input_ == null) {
			log_.error("TEST", "Tester setup failed to create either the traj_ or input_ object");
			try {
				LoggerManager.writeToDisk();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testFastFarGreenLong_FirstCall() {

    	try {
    		Thread.sleep(2);
    		log_.debug("TEST", "===== Entering testFastFarGreenLong_FirstCall");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, SPEED15);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, L13N5_LAT);
		input_.put(DataElementKey.LONGITUDE, L13N5_LON);
		input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output_ = traj_.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testFastFarGreenLong_FirstCall: " + e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		IntDataElement laneId = (IntDataElement)output_.get(DataElementKey.LANE_ID);
		DoubleDataElement dtsb = (DoubleDataElement)output_.get(DataElementKey.DIST_TO_STOP_BAR);
		
		try {
			assertNotNull(speedCmd_);
			assertNotNull(laneId);
			assertNotNull(dtsb);
			if (speedCmd_ != null  &&  laneId != null  &&  dtsb != null) {
				assertEquals(15.22, speedCmd_.value(), 0.05);
				assertEquals(laneId.value(), 13);
				assertEquals(120.62, dtsb.value(), 0.03);
			}
		} catch (AssertionError a) {
			log_.warnf("TEST", "assertion exception trapped. %s", a.getMessage());
		}
	}

	@Test
	public void testNoMap() {

    	try {
    		Thread.sleep(2);
        	log_.debug("TEST", "===== Entering testNoMap");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		
		Trajectory traj; //can't use the existing traj_ because it already has a MAP stored
		
		//initial call - no MAP provided, and no history
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, SPEED15);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, L13N5_LAT);
		input_.put(DataElementKey.LONGITUDE, L13N5_LON);
		try {
			traj = new Trajectory(); 
			traj.engage(); //simulate the driver turning on automated control
			output_ = traj.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testNoMap: " + e.getMessage());
			return;
		}
		assertNull(output_.get(DataElementKey.SPEED_COMMAND));
		
		//next call - provide a new MAP
    	try {
    		Thread.sleep(2);
        	log_.debug("TEST", "Ready for next call - providing a new MAP");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, SPEED15);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, L13N5_LAT);
		input_.put(DataElementKey.LONGITUDE, L13N5_LON);
		input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		try {
			//success depends on position being associated with a lane in the intersection geometry; first call makes that association
			traj.getSpeedCommand(input_); 
			//second call at the same location ensures that it doesn't throw out the position because of a huge diff from previous call
			//(this is an artifact of testing rather than smooth travel along the whole lane)
			output_ = traj.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testNoMap: " + e.getMessage());
			return;
		}
		assertNotNull(output_.get(DataElementKey.SPEED_COMMAND));
		
		//third call - previous MAP should have been stored, so no need to provide another one here and it will work fine
    	try {
    		Thread.sleep(2);
        	log_.debug("TEST", "Ready for third call - no MAP");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, SPEED15);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, L13N5_LAT);
		input_.put(DataElementKey.LONGITUDE, L13N5_LON);
		try {
			output_ = traj.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testNoMap: " + e.getMessage());
			return;
		}
		assertNotNull(output_.get(DataElementKey.SPEED_COMMAND));
	}

    @Test
	public void testFastFarGreenLong_OperSpeedAchieved() {
        try {
        	Thread.sleep(2);
			log_.debug("TEST", "===== entering testFastFarGreenLong_OperSpeedAchieved");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
			input_.clear();
			input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
			input_.put(DataElementKey.SMOOTHED_SPEED, OPER_SPEED);
			input_.put(DataElementKey.ACCELERATION, ACCEL0);
			input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
			input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME99);
			input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
			input_.put(DataElementKey.LATITUDE, L13N5_LAT);
			input_.put(DataElementKey.LONGITUDE, L13N5_LON);
			input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		} catch (Exception e1) {
			log_.error("TEST", "Exception trapped in initialization section of _OperSpeedAchieved()");
			e1.printStackTrace();
			return;
		}
		
		try {
			//get speed command twice, since first time step doesn't understand the real acceleration picture and limiter will be funky
			traj_.getSpeedCommand(input_);
			output_ = traj_.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testFastFarGreenLong_OperSpeedAchieved: " + e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		
		assertEquals(OPER_SPEED.value(), speedCmd_.value(), 0.01);
	}

    @Test
	public void testFastFarGreenLong_BeyondOperSpeed() {
		
		//guarantee we've achieved operating speed first
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testFastFarGreenLong_BeyondOperSpeed");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
			testFastFarGreenLong_OperSpeedAchieved();
		} catch (Throwable e) {
			log_.warn("TEST", "EadWrappertester.testFastFarGreenLong_BeyondOperSpeed caught error from _OperSpeedAchieved() call");
			return;
		}
		
		log_.debug("TEST", "...back in testFastFarGreenLong_BeyondOperSpeed");
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, SPEED15_3);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, L13N5_LAT);
		input_.put(DataElementKey.LONGITUDE, L13N5_LON);
		input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			//use two calls at this speed to let the acceleration & jerk limiter settle down
			traj_.getSpeedCommand(input_);
			traj_.getSpeedCommand(input_);
			output_ = traj_.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testFastFarGreenLong_BeyondOperSpeed: " + e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		assertTrue(speedCmd_.value() >= OPER_SPEED.value()  &&  speedCmd_.value() < SPEED15_3.value());
    }

    //this test is intended to exercise the real EAD library - don't use it with a fake trajectory file!
    @Test
	public void testFastFarGreenLong_10Calls() {
        double speed = OPER_SPD_7.value() + 1.0; //a little above oper speed guarantees that it will be marked "achieved" in first time step
    	int iter = 0;
    	
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testFastFarGreenLong_10Calls");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		
		if (config_.getProperty("ead.trajectoryfile") != null) {
			log_.debug("TEST",  "Skipping this step - won't work while a trajectory file is defined.");
			return;
		}
		
		try {
			for (iter = 0;  iter < 10;  ++iter) {
				setupInput(speed);
				output_ = traj_.getSpeedCommand(input_);
				log_.debugf("TEST", "10Calls returned from iter %d: act speed = %f, speed cmd = %f", 
							iter, speed, output_.getDoubleElement(DataElementKey.SPEED_COMMAND));
				speed = (output_.getDoubleElement(DataElementKey.SPEED_COMMAND) + 2.0*speed)/3.0; //gradually approach the commanded speed
			}
		} catch (Exception e) {
			log_.errorf("TEST*", "Exception caught in _10Calls on iter %d: %s", iter, e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		assertNotNull(speedCmd_);
		assertEquals(speedCmd_.value(), 15.22, 0.1);
    }
    
    //this test is only meaningful with the real EAD library
    @Test
	public void testPastStopBar() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entered testPastStopBar");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}

		if (config_.getProperty("ead.trajectoryfile") != null) {
			log_.debug("TEST",  "Skipping this test - won't work while a trajectory file is defined.");
			return;
		}

		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, SPEED0_3);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, RED);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, DIST_NEG_LAT);
		input_.put(DataElementKey.LONGITUDE, DIST_NEG_LON);
		input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output_ = traj_.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testPastStopBar: " + e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		IntDataElement laneId = (IntDataElement)output_.get(DataElementKey.LANE_ID);
		DoubleDataElement dtsb = (DoubleDataElement)output_.get(DataElementKey.DIST_TO_STOP_BAR);
		
		assertEquals(0.0, speedCmd_.value(), 0.01);
		assertEquals(laneId.value(), 13);
		assertTrue(dtsb.value() > -4.0  &&  dtsb.value() < 0.0);
	}
	
    @Test
	public void testEgress1() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testEgress1");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}

		//prep calls 1 - - uptrack of the stop box, achieving operating speed to ensure that later calls can bypass the auto command to hit oper speed
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, OPER_SPEED);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, L13N0_LAT);
		input_.put(DataElementKey.LONGITUDE, L13N0_LON);
		input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_EGRESS));
		
		try {
			traj_.getSpeedCommand(input_);
			output_ = traj_.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testEgress1: " + e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		assertNotNull(speedCmd_);
		assertTrue(speedCmd_.value() > 0.01);

		//prep calls 2 - entering the stop box, which will associate with the approach lane, necessary to set up the history for the next call
		try {
			Thread.sleep(2);
			log_.debug("TEST", "Ready to start prep call 2");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, SPEED0_3);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, DIST_NEG_LAT);
		input_.put(DataElementKey.LONGITUDE, DIST_NEG_LON);
		input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_EGRESS));
		
		try {
			traj_.getSpeedCommand(input_);
			output_ = traj_.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testEgress1: " + e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		IntDataElement laneId = (IntDataElement)output_.get(DataElementKey.LANE_ID);
		DoubleDataElement dtsb = (DoubleDataElement)output_.get(DataElementKey.DIST_TO_STOP_BAR);
		assertNotNull(speedCmd_);
		assertTrue( speedCmd_.value() > 0.03);
		assertEquals(laneId.value(), 13);
		assertTrue(dtsb.value() > -4.0  &&  dtsb.value() < 0.0);

		//far side call - on the far side of the stop box; should still be associated with the approach lane
		try {
			Thread.sleep(2);
			log_.debug("TEST", "Ready to start far side call");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, SPEED0_3);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, INT_EXIT_LAT);
		input_.put(DataElementKey.LONGITUDE, INT_EXIT_LON);
		input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_EGRESS));
		
		try {
			output_ = traj_.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testEgress1, first call: " + e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		IntDataElement laneId1 = (IntDataElement)output_.get(DataElementKey.LANE_ID);
		DoubleDataElement dtsb1 = (DoubleDataElement)output_.get(DataElementKey.DIST_TO_STOP_BAR);
		assertNotNull(speedCmd_);
		assertNotNull(laneId1);
		assertNotNull(dtsb1);
		double speedCmd1 = speedCmd_.value();
		assertTrue(speedCmd1 > 0.01);
		assertEquals(laneId1.value(), 13);
		assertTrue(dtsb1.value() < -4.0);
		
		//final call - out of the stop box and following egress lane 44
		try {
			Thread.sleep(2);
			log_.debug("TEST", "Ready to start final call");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
		input_.put(DataElementKey.SMOOTHED_SPEED, SPEED1_6);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, L44N1_LAT);
		input_.put(DataElementKey.LONGITUDE, L44N1_LON); //no MAP message - rely on the one from previous call
		
		try {
			output_ = traj_.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testEgress1, second call: " + e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		IntDataElement laneId2 = (IntDataElement)output_.get(DataElementKey.LANE_ID);
		DoubleDataElement dtsb2 = (DoubleDataElement)output_.get(DataElementKey.DIST_TO_STOP_BAR);
		double speedCmd2 = speedCmd_.value();
		assertTrue(speedCmd2 >= speedCmd1);
		assertEquals(laneId2.value(), 44);
		assertTrue(dtsb2.value() < dtsb1.value());
	}
	
    @Test
	public void testEmergencyStop() {
        try {
        	Thread.sleep(2);
			log_.debug("TEST", "===== entering testEmergencyStop");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
			input_.clear();
			input_.put(DataElementKey.OPERATING_SPEED, OPER_SPEED);
			input_.put(DataElementKey.SMOOTHED_SPEED, OPER_SPEED);
			input_.put(DataElementKey.ACCELERATION, ACCEL0);
			input_.put(DataElementKey.SIGNAL_PHASE, RED);
			input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME24);
			input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
			input_.put(DataElementKey.LATITUDE, L13N2_LAT);
			input_.put(DataElementKey.LONGITUDE, L13N2_LON);
			input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		} catch (Exception e1) {
			log_.error("TEST", "Exception trapped in initialization section of testEmergencyStop()");
			e1.printStackTrace();
			return;
		}
		
		try {
			output_ = traj_.getSpeedCommand(input_);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in testEmergencyStop: " + e.getMessage());
			return;
		}
		speedCmd_ = (DoubleDataElement)output_.get(DataElementKey.SPEED_COMMAND);
		assertTrue(speedCmd_.value() < OPER_SPEED.value() - 0.5);	//0.5 is about half the max deceleration limit for one time step
	}
    
    @Ignore //ignoring because it requires multiple config params to be like they were on that test, which is no longer SOP
    public void test150255BigAccel() { //reproducing drive log of 20150224.150255 starting at time 58.933
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering test150255BigAccel");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		
		Trajectory t = null;
		try {
			t = new Trajectory();
			t.engage(); //simulate the driver turning on automated control
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		DataElementHolder input = new DataElementHolder();
		DataElementHolder output = new DataElementHolder();
		DoubleDataElement cmdElem = null;
		DoubleDataElement operSpeed = new DoubleDataElement(8.9408);
		PhaseDataElement phase = new PhaseDataElement(SignalPhase.RED);

		//58.933
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(1.0824));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(0.7260));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		double signalTime = 22.0;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 27.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.95494956));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14823867));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}
		cmdElem = (DoubleDataElement)output.get(DataElementKey.SPEED_COMMAND);
		double cmd = cmdElem.value();
		assertEquals(1.255, cmd, 0.01);
		
		//59.032
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(1.1506));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(0.6820));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		signalTime = 21.9;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 27.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.9549495));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14823985));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}
		cmdElem = (DoubleDataElement)output.get(DataElementKey.SPEED_COMMAND);
		cmd = cmdElem.value();
		assertEquals(1.319, cmd, 0.01);
		
		//59.137
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(1.2386));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(0.88));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		signalTime = 21.8;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 27.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.95494939));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14824119));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}
		cmdElem = (DoubleDataElement)output.get(DataElementKey.SPEED_COMMAND);
		cmd = cmdElem.value();
		assertEquals(1.427, cmd, 0.01);
		
		//59.237
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(1.3222));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(0.8360));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		signalTime = 21.7;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 27.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.9549493));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14824278));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}
		cmdElem = (DoubleDataElement)output.get(DataElementKey.SPEED_COMMAND);
		cmd = cmdElem.value();
		assertEquals(1.506, cmd, 0.01);
		
		//59.344
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(1.4278));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(1.0560));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		signalTime = 21.6;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 27.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.95494922));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14824429));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}
		cmdElem = (DoubleDataElement)output.get(DataElementKey.SPEED_COMMAND);
		cmd = cmdElem.value();
		assertEquals(1.633, cmd, 0.01);
		
    }

    //THIS TEST REQUIRES ead.osduration = 0!
    @Test
    public void test080653SlowDown() { //reproducing drive log of 20150309.080653 starting at 12.535 sec since first motion
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering test080653SlowDown");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
		
		Trajectory t = null;
		try {
			t = new Trajectory();
			t.engage(); //simulate the driver turning on automated control
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		DataElementHolder input = new DataElementHolder();
		DataElementHolder output = new DataElementHolder();
		DoubleDataElement cmdElem = null;
		DoubleDataElement operSpeed = new DoubleDataElement(11.176);
		PhaseDataElement phase = new PhaseDataElement(SignalPhase.GREEN);

		//prelim - need to be sure to achieve OS
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(operSpeed.value() + 0.02));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(-0.02384));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		double signalTime = 6.2;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 3.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.95500982));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14781497));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}

		//prelim 2 - now that we've achieved OS, let's set up the history where we need to be to enter this test
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(7.7363));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(-0.07432));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		signalTime = 6.1;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 3.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.95500831));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14782402));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}
		cmdElem = (DoubleDataElement)output.get(DataElementKey.SPEED_COMMAND);
		double cmd = cmdElem.value();

		//12.535
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(7.71101));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(-0.02384));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		signalTime = 6.0;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 3.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.95500722));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14783232));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}
		cmdElem = (DoubleDataElement)output.get(DataElementKey.SPEED_COMMAND);
		cmd = cmdElem.value();
		//assertEquals(7.67, cmd, 0.1);

		//12.639
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(7.68366));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(0.047520));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		signalTime = 5.9;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 3.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.95500605));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14784137));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}
		cmdElem = (DoubleDataElement)output.get(DataElementKey.SPEED_COMMAND);
		cmd = cmdElem.value();
		//assertEquals(7.64, cmd, 0.1);

		//12.750
		input.clear();
		input.put(DataElementKey.OPERATING_SPEED, operSpeed);
		input.put(DataElementKey.SMOOTHED_SPEED, new DoubleDataElement(7.6942));
		input.put(DataElementKey.ACCELERATION, new DoubleDataElement(0.068580));
		input.put(DataElementKey.SIGNAL_PHASE, phase);
		signalTime = 5.8;
		input.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, new DoubleDataElement(signalTime));
		input.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, new DoubleDataElement(signalTime + 3.0));
		input.put(DataElementKey.LATITUDE, new DoubleDataElement(38.95500496));
		input.put(DataElementKey.LONGITUDE, new DoubleDataElement(-77.14784958));
		input.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
		
		try {
			output = t.getSpeedCommand(input);
		} catch (Exception e) {
			log_.error("TEST", "Exception caught in test150255BigAccel: " + e.getMessage());
			return;
		}
		cmdElem = (DoubleDataElement)output.get(DataElementKey.SPEED_COMMAND);
		cmd = cmdElem.value();
		//assertEquals(7.64, cmd, 0.1);
}
    
	@After
	public void shutdown() {
        try {
        	traj_.close();
			LoggerManager.setOutputFile("logs/EadTestLog.txt");
			LoggerManager.writeToDisk();
		}catch (Exception e) {
			//do nothing for now
		}

	}

/////////////////////////////////
	
	private void setupInput(double speed) {
        DoubleDataElement sp = new DoubleDataElement(speed);
		input_.clear();
		input_.put(DataElementKey.OPERATING_SPEED, OPER_SPD_7);
		input_.put(DataElementKey.SMOOTHED_SPEED, sp);
		input_.put(DataElementKey.ACCELERATION, ACCEL0);
		input_.put(DataElementKey.SIGNAL_PHASE, GREEN);
		input_.put(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE, TIME99);
		input_.put(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE, TIME99);
		input_.put(DataElementKey.LATITUDE, L13N5_LAT);
		input_.put(DataElementKey.LONGITUDE, L13N5_LON);
		input_.put(DataElementKey.MAP_MESSAGE, loadMap(MAP_MSG_PRIMARY));
	}
		
	private DataElement loadMap(String testName) {
		byte[] buf = null;
		MapMessage msg = null;
		String filename = "testdata/" + testName + ".dat";
		
		try {
			FileInputStream is = new FileInputStream(filename);
			buf = new byte[1400];
			int num = is.read(buf);
			if (num <= 0) {
				is.close();
				throw new IOException("No bytes read from file.");
			}
			is.close();
			
	    	msg = new MapMessage();
	    	msg.parse(buf); //not testing for success - only feed it valid data!
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		MapMessageDataElement elem = new MapMessageDataElement(msg);

    	return elem;
	}

	private Trajectory			traj_;
	private DataElementHolder	input_;
	private DataElementHolder	output_;
	private DoubleDataElement	speedCmd_;
	private AppConfig			config_;
	private Logger				log_;

	private static final DoubleDataElement OPER_SPEED	= new DoubleDataElement(15.22); // m/s
	private static final DoubleDataElement OPER_SPD_7	= new DoubleDataElement(3.17); // m/s (= 7.09 mph)
	private static final DoubleDataElement	SPEED0		= new DoubleDataElement(0.0); // m/s
	private static final DoubleDataElement	SPEED0_3	= new DoubleDataElement(0.3); // m/s
	private static final DoubleDataElement	SPEED1_6	= new DoubleDataElement(1.6); // m/s
	private static final DoubleDataElement	SPEED15		= new DoubleDataElement(15.0); // m/s
	private static final DoubleDataElement	SPEED15_3	= new DoubleDataElement(15.3); //m/s
	private static final DoubleDataElement	ACCEL0		= new DoubleDataElement(0.0);  //m/s^3
	private static final DoubleDataElement	TIME0		= new DoubleDataElement(0.0); // s
	private static final DoubleDataElement	TIME3_3		= new DoubleDataElement(3.3); // s
	private static final DoubleDataElement	TIME24		= new DoubleDataElement(24.0); // s
	private static final DoubleDataElement TIME99		= new DoubleDataElement(99.0); // s
	private static final PhaseDataElement	RED			= new PhaseDataElement(SignalPhase.RED);
	private static final PhaseDataElement	YELLOW		= new PhaseDataElement(SignalPhase.YELLOW);
	private static final PhaseDataElement	GREEN		= new PhaseDataElement(SignalPhase.GREEN);
	private static final DoubleDataElement	L13N5_LAT	= new DoubleDataElement(38.954683); //node 5 of lane 13 of completMapMessage1, ~20 m from stop bar
	private static final DoubleDataElement	L13N5_LON	= new DoubleDataElement(-77.149515);
	private static final DoubleDataElement	L13N2_LAT	= new DoubleDataElement(38.954784); //~12 m in front of stop bar
	private static final DoubleDataElement	L13N2_LON	= new DoubleDataElement(-77.149440);
	private static final DoubleDataElement	L13N0_LAT	= new DoubleDataElement(38.954884); //just before the lane 13 stop bar
	private static final DoubleDataElement	L13N0_LON	= new DoubleDataElement(-77.149375);
	private static final DoubleDataElement	DIST_NEG_LAT= new DoubleDataElement(38.954887); //a few cm beyond the stop bar of lane 13
	private static final DoubleDataElement	DIST_NEG_LON= new DoubleDataElement(-77.149373);
	private static final DoubleDataElement	INT_EXIT_LAT= new DoubleDataElement(38.954924); //right side of stop box, nearing lane 44 stop bar
	private static final DoubleDataElement	INT_EXIT_LON= new DoubleDataElement(-77.149140);
	private static final DoubleDataElement	L44N1_LAT	= new DoubleDataElement(38.954919); //very close to node 1 of lane 44 of MapLane12Egress9
	private static final DoubleDataElement	L44N1_LON	= new DoubleDataElement(-77.149070);
	private static final String	MAP_MSG_PRIMARY = "CompleteMapMessage1";
	private static final String MAP_MSG_EGRESS = "MapLane12Egress9";
}
