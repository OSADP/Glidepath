package com.leidos.glidepath.ead;

import static org.junit.Assert.*;

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
import com.leidos.glidepath.dvi.AppConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class EadUcrJavaTester {
	
    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void setup() {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
        config_ = context.getAppConfig();
        LoggerManager.setRecordData(true);
    }
    
    @Test
	public void testStopped() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testStopped");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadUcrJava ead = new EadUcrJava();
		ead.initialize(100, 0);
		
		try {
			ead.setState(0.0, SPEED_MED, 0.0, DIST_VERY_CLOSE, SignalPhase.RED.value(), 12.0, 30.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts = ead.getTargetSpeed();
		assertEquals(ts, 0.0, 0.001);
	}
    
    @Test
	public void testDeparting() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testDeparting");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadUcrJava ead = new EadUcrJava();
		ead.initialize(100, 0);
		
		//setup - need to get the scenario state machine into GRADUAL_STOP first
		try {
			ead.setState(1.0, SPEED_MED, 1.0, DIST_VERY_CLOSE+2.0, SignalPhase.RED.value(), 1.0, 30.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts = ead.getTargetSpeed();
		assertTrue(ts > 0.0);
		
		//setup - this should transition the scenario to FINAL_STOP
		try {
			ead.setState(0.0, SPEED_MED, 0.0, DIST_VERY_CLOSE, SignalPhase.RED.value(), 0.7, 29.7);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		ts = ead.getTargetSpeed();
		assertEquals(ts, 0.0, 0.001);
		
		//now that we're stopped, need to stay here for 5 time steps to convince the scenario machine that it's for real
		ts = ead.getTargetSpeed();
		ts = ead.getTargetSpeed();
		ts = ead.getTargetSpeed();
		ts = ead.getTargetSpeed();
		ts = ead.getTargetSpeed();
		
		//first call - Stopped at the stop bar - forces the state to be changed to departing
		try {
			ead.setState(0.0, SPEED_MED, 0.0, DIST_VERY_CLOSE, SignalPhase.GREEN.value(), 30.0, 3.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		ts = ead.getTargetSpeed();
		assertTrue(ts > 0.0);
		
		//second call - driving through the intersection
		try {
			ead.setState(SPEED_CRAWL, SPEED_MED, 0.0, DIST_IN_STOP_BOX, SignalPhase.GREEN.value(), 30.0, 33.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts2 = ead.getTargetSpeed();
		assertTrue(ts2 > SPEED_CRAWL);
		
		//third call - departed the intersection
		try {
			ead.setState(SPEED_SLOW, SPEED_MED, 0.0, DIST_EGRESS, SignalPhase.GREEN.value(), 12.0, 15.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts3 = ead.getTargetSpeed();
		assertTrue(ts3 > SPEED_SLOW);
    }
    
    @Test
	public void testCruiseFarGreen() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testCruiseFarGreen");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadUcrJava ead = new EadUcrJava();
		ead.initialize(100, 0);
		
		try {
			ead.setState(SPEED_MED_PLUS, SPEED_MED, 0.0, DIST_FAR, SignalPhase.GREEN.value(), 70.0, 99.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts = ead.getTargetSpeed();
		assertTrue(ts < SPEED_MED_PLUS  &&  ts >= SPEED_MED);
	}
    
    @Test
    public void testCruiseMedGreen() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testCruiseMedGreen");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadUcrJava ead = new EadUcrJava();
		ead.initialize(100, 0);
		
		try {
			ead.setState(SPEED_MED_MINUS, SPEED_MED, 0.0, DIST_MED, SignalPhase.GREEN.value(), 20.0, 25.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts = ead.getTargetSpeed();
		assertTrue(ts > SPEED_MED_MINUS  &&  ts <= SPEED_MED);
    }
    
    @Test
    public void testAccelMedGreen() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testAccelMedGreen");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadUcrJava ead = new EadUcrJava();
		ead.initialize(100, 0);
		
		//at 11.1 m/s we will reach stop bar in 8.1 s. At 15.6 m/s (speed limit) we will reach it in 5.8 s.
		//run through several time steps to watch the acceleration progress
		final int LOOP_LIMIT = 30;
		double speed = SPEED_MED_MINUS;
		double dist = DIST_MED;
		double time1 = 7.9;
		double ts = 0.0;
		for (int i = 0;  i < LOOP_LIMIT;  ++i) {
			try {
				ead.setState(speed, SPEED_MED, 0.0, dist, SignalPhase.GREEN.value(), time1, time1+3.0);
			} catch (Exception e) {
				log_.error("TEST", "Exception thrown from setState: " + e.toString());
				assertTrue(false);
			}
			try {
				ts = ead.getTargetSpeed();
			} catch (Exception e) {
				//exception handler not normally needed for this method, but trying to trap something that has been unhandled
				log_.errorf("TEST", "Trapped exception from getTargetSpeed on iteration %d: %s", i, e.toString());
				e.printStackTrace();
			}
			log_.debugf("TEST", "Iter %d returned ts = %.4f", i, ts);
			if (i < 20){
				assertTrue(ts <= SPEED_LIMIT  &&  ts >= speed);
			}else {
				assertTrue(ts <= SPEED_LIMIT  &&  ts > SPEED_MED);
			}
			
			speed = ts;
			dist -= 0.1*speed;
			time1 -= 0.1;
		}
    }
    
    @Test
    public void testUnableToAccel() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testUnableToAccel");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadUcrJava ead = new EadUcrJava();
		ead.initialize(100, 0);
		
		//at 11.1 m/s we will reach stop bar in 8.1 s. At 15.6 m/s (speed limit) we will reach it in 5.8 s.
		// At 2.2 m/s (crawling) we will reach it in 40.4 s.
		try {
			ead.setState(SPEED_MED_MINUS, SPEED_MED, 0.0, DIST_MED, SignalPhase.GREEN.value(), 5.7, 8.7);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts = ead.getTargetSpeed();
		log_.debugf("TEST", "Returned ts = %.4f", ts);
		assertTrue(ts <= SPEED_MED_MINUS  &&  ts > SPEED_CRAWL);
		
		//second time step to be sure we're slowing down
		try {
			ead.setState(ts, SPEED_MED, 0.0, DIST_MED, SignalPhase.GREEN.value(), 5.6, 8.6);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts2 = ead.getTargetSpeed();
		log_.debugf("TEST", "Returned ts = %.4f", ts2);
		assertTrue(ts2 < ts  &&  ts2 > SPEED_CRAWL);
    }
    
    @Test
    public void testDecelFarRed() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testDecelFarRed");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadUcrJava ead = new EadUcrJava();
		ead.initialize(100, 0);
		
		//first time step - at 7.5 m/s we will reach stop bar in 24.0 s. At 2.24 m/s we will reach it in 80.3 s.
		try {
			ead.setState(SPEED_SLOW_MINUS, SPEED_SLOW, 0.0, DIST_FAR, SignalPhase.RED.value(), 30.0, 60.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts = ead.getTargetSpeed();
		log_.debugf("TEST", "First call returned ts = %.4f", ts);
		assertTrue(ts <= SPEED_SLOW_MINUS  &&  ts > SPEED_CRAWL);
		
		//second time step
		double dist = DIST_FAR - ts*0.1;
		try {
			ead.setState(ts, SPEED_SLOW, 0.0, dist, SignalPhase.RED.value(), 29.9, 59.9);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts2 = ead.getTargetSpeed();
		log_.debugf("TEST", "Second call returned ts = %.4f", ts2);
		assertTrue(ts2 < ts  &&  ts2 > SPEED_CRAWL);

		//third time step
		dist -= ts2*0.1;
		try {
			ead.setState(ts2, SPEED_SLOW, 0.0, dist, SignalPhase.RED.value(), 29.8, 59.8);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts3 = ead.getTargetSpeed();
		log_.debugf("TEST", "Third call returned ts = %.4f", ts3);
		assertTrue(ts3 < ts2  &&  ts3 > SPEED_CRAWL);
    }
    
    @Test
    public void testStopMedYellow() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testStopMedYellow");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadUcrJava ead = new EadUcrJava();
		ead.initialize(100, 0);
		
		//first time step - should see a gradual deceleration to a stop at the bar
		try {
			//at 20 m/s we will reach stop bar in 4.5 s. At 2.24 m/s we will reach it in 40.2 s, but it will take time to slow down that far
			ead.setState(SPEED_MED_PLUS, SPEED_MED, 0.0, DIST_MED, SignalPhase.YELLOW.value(), 4.0, 36.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts = ead.getTargetSpeed();
		log_.debugf("TEST", "First call returned ts = %.4f", ts);
		assertTrue(ts <= SPEED_MED_PLUS  &&  ts > SPEED_CRAWL);

		//second time step
		double dist = DIST_MED - ts*0.1;
		try {
			ead.setState(ts, SPEED_MED, 0.0, dist, SignalPhase.YELLOW.value(), 3.9, 35.9);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts2 = ead.getTargetSpeed();
		log_.debugf("TEST", "Second call returned ts = %.4f", ts2);
		assertTrue(ts2 < ts  &&  ts2 > SPEED_CRAWL);

		//third time step
		dist -= ts2*0.1;
		try {
			ead.setState(ts2, SPEED_MED, 0.0, dist, SignalPhase.YELLOW.value(), 3.8, 35.8);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts3 = ead.getTargetSpeed();
		log_.debugf("TEST", "Third call returned ts = %.4f", ts3);
		assertTrue(ts3 < ts2  &&  ts3 > SPEED_CRAWL);
    }
    
    @Test
    public void testStopNearBar() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testStopNearBar");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadUcrJava ead = new EadUcrJava();
		ead.initialize(100, 0);
		
		//loop through several time steps taking it right up to the stop bar
		double dist = 3.1;
		double time1 = 14.0;
		double time2 = 44.0;
		double speed = SPEED_CRAWL;
		for (int iter = 0;  iter < 60  &&  speed > 0.0;  ++iter) {
			try {
				ead.setState(speed, SPEED_MED, 0.0, dist, SignalPhase.RED.value(), time1, time2);
			} catch (Exception e) {
				log_.error("TEST", "Exception thrown from setState: " + e.toString());
				assertTrue(false);
			}
			double ts = ead.getTargetSpeed();
			log_.debugf("TEST", "Call %d returned ts = %.2f", iter, ts);
			if (iter > 0) {
				assertTrue(ts < speed);
			}
			
			//reset for the next time step
			speed = ts;
			time1 -= 0.1;
			time2 -= 0.1;
			dist -= 0.1*ts;
		}
    }
    
    @After
    public void shutdown() {
		try {
			LoggerManager.setOutputFile("logs/EadUcrJava.txt");
			LoggerManager.writeToDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    //////////////////
    // private members
    //////////////////
    
    private static final double	SPEED_LIMIT				= 35.0/Constants.MPS_TO_MPH;
    private static final double	SPEED_MED				= 11.1;
    private static final double	SPEED_MED_PLUS			= 11.4;
    private static final double	SPEED_MED_MINUS			= 10.9;
    private static final double SPEED_SLOW				= 7.5;
    private static final double SPEED_SLOW_MINUS		= 7.4;
    private static final double SPEED_CRAWL				= 1.0;
    private static final double	DIST_FAR				= 180.0;
    private static final double	DIST_MED				= 90.0;
    private static final double DIST_CLOSE				= 11.5;
    private static final double DIST_VERY_CLOSE			= 0.93;
    private static final double DIST_IN_STOP_BOX		= -2.2;
    private static final double DIST_EGRESS				= -34.0;
    
    private AppConfig			config_;
	private static Logger		log_ = (Logger)LoggerManager.getLogger(EadUcrJavaTester.class);
}
