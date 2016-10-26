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
public class EadSimpleTester {
	
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
		EadSimple ead = new EadSimple();
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
		EadSimple ead = new EadSimple();
		ead.initialize(100, 0);
		
		//first call - Stopped at the stop bar - forces the state to be changed to departing
		try {
			ead.setState(0.0, SPEED_MED, 0.0, DIST_VERY_CLOSE, SignalPhase.RED.value(), 1.0, 30.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts = ead.getTargetSpeed();
		assertEquals(ts, 0.0, 0.001);
		
		//second call - driving through the intersection
		try {
			ead.setState(SPEED_CRAWL, SPEED_MED, 0.0, DIST_IN_STOP_BOX, SignalPhase.GREEN.value(), 30.0, 3.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts2 = ead.getTargetSpeed();
		assertTrue(ts2 > 0.0);
		
		//third call - departed the intersection
		try {
			ead.setState(SPEED_SLOW, SPEED_MED, 0.0, DIST_EGRESS, SignalPhase.GREEN.value(), 12.0, 3.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts3 = ead.getTargetSpeed();
		assertEquals(SPEED_MED, ts3, 0.001);
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
		EadSimple ead = new EadSimple();
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
		EadSimple ead = new EadSimple();
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
		EadSimple ead = new EadSimple();
		ead.initialize(100, 0);
		
		//first time step - at 7.5 m/s we will reach stop bar in 12.0 s. At 15.6 m/s (speed limit) we will reach it in 5.8 s.
		try {
			ead.setState(SPEED_SLOW_MINUS, SPEED_SLOW, 0.0, DIST_MED, SignalPhase.GREEN.value(), 11.8, 14.9);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts = ead.getTargetSpeed();
		log_.debugf("TEST", "First call returned ts = %.2f", ts);
		assertTrue(ts < SPEED_LIMIT  &&  ts > SPEED_SLOW_MINUS);
		
		//second time step
		double dist = DIST_MED - ts*0.1;
		double s2 = 0.5*(ts + SPEED_SLOW_MINUS);
		try {
			ead.setState(s2, SPEED_SLOW, 0.0, dist, SignalPhase.GREEN.value(), 11.7, 14.8);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts2 = ead.getTargetSpeed();
		log_.debugf("TEST", "Second call returned ts = %.2f", ts2);
		assertTrue(ts2 > s2  &&  ts2 < SPEED_LIMIT);

		//third time step
		dist -= ts2*0.1;
		double s3 = 0.5*(s2 + ts2);
		try {
			ead.setState(s3, SPEED_SLOW, 0.0, dist, SignalPhase.GREEN.value(), 11.6, 14.7);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts3 = ead.getTargetSpeed();
		log_.debugf("TEST", "Third call returned ts = %.2f", ts3);
		assertTrue(ts3 > s3  &&  ts3 < SPEED_LIMIT);
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
		EadSimple ead = new EadSimple();
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
		log_.debugf("TEST", "Returned ts = %.2f", ts);
		assertTrue(ts < SPEED_MED_MINUS  &&  ts > SPEED_CRAWL);
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
		EadSimple ead = new EadSimple();
		ead.initialize(100, 0);
		
		//first time step - at 7.5 m/s we will reach stop bar in 24.0 s. At 2.24 m/s we will reach it in 80.3 s.
		try {
			ead.setState(SPEED_SLOW_MINUS, SPEED_SLOW, 0.0, DIST_FAR, SignalPhase.RED.value(), 30.0, 60.0);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		ead.getTargetSpeed(); //use one to let the jerk limiter settle down
		double ts = ead.getTargetSpeed();
		log_.debugf("TEST", "First call returned ts = %.2f", ts);
		assertTrue(ts < SPEED_SLOW_MINUS  &&  ts > SPEED_CRAWL);
		
		//second time step
		double dist = DIST_FAR - ts*0.1;
		try {
			ead.setState(ts, SPEED_SLOW, 0.0, dist, SignalPhase.RED.value(), 29.9, 59.9);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts2 = ead.getTargetSpeed();
		log_.debugf("TEST", "Second call returned ts = %.2f", ts2);
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
		log_.debugf("TEST", "Third call returned ts = %.2f", ts3);
		assertTrue(ts3 < ts2  &&  ts3 > SPEED_CRAWL);
    }
    
    @Test
    public void testStopMedYellow() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testDecelFarRed");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadSimple ead = new EadSimple();
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
		log_.debugf("TEST", "First call returned ts = %.2f", ts);
		assertTrue(ts < SPEED_MED_PLUS  &&  ts > SPEED_CRAWL);

		//second time step
		double dist = DIST_MED - ts*0.1;
		try {
			ead.setState(ts, SPEED_MED, 0.0, dist, SignalPhase.YELLOW.value(), 3.9, 35.9);
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
		double ts2 = ead.getTargetSpeed();
		log_.debugf("TEST", "Second call returned ts = %.2f", ts2);
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
		log_.debugf("TEST", "Third call returned ts = %.2f", ts3);
		assertTrue(ts3 < ts2  &&  ts3 > SPEED_CRAWL);
    }
    
    @After
    public void shutdown() {
		try {
			LoggerManager.setOutputFile("logs/EadSimple.txt");
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
    private static final double DIST_IN_STOP_BOX		= -4.2;
    private static final double DIST_EGRESS				= -34.0;
    
    private AppConfig			config_;
	private static Logger		log_ = (Logger)LoggerManager.getLogger(EadSimpleTester.class);
}
