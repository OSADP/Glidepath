package com.leidos.glidepath.ead;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.Logger;
import com.leidos.glidepath.logger.LoggerManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class AccelerationManagerTester {
	
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
	public void testEarlyRampUp() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testEarlyRampUp");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
		AccelerationManager am = AccelerationManager.getManager();
		am.currentScenarioIs(Scenario.RAMP_UP, 0.255, 11.0);
		double limit = am.getAccelLimit();
		assertEquals(1.84, limit, 0.0001);
	}
	
	@Test
	public void testLateRampUp() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testLateRampUp");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
		AccelerationManager am = AccelerationManager.getManager();
		am.currentScenarioIs(Scenario.RAMP_UP, 11.1, 11.0);
		double limit = am.getAccelLimit();
		assertEquals(2.0, limit, 0.0001);
	}
	
	@Test
	public void testSpeedup() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testSpeedup");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
		AccelerationManager am = AccelerationManager.getManager();
		am.currentScenarioIs(Scenario.OVERSPEED, 11.4, 11.0);
		double limit = am.getAccelLimit();
		assertEquals(1.81, limit, 0.0001);
	}
	
	@Test
	public void testStop() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testStop");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
		AccelerationManager am = AccelerationManager.getManager();
		am.currentScenarioIs(Scenario.FINAL_STOP, 2.33, 0.0);
		double limit = am.getAccelLimit();
		assertEquals(1.82, limit, 0.0001);
	}
	
	@Test
	public void testSpeedupDeparture() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testSpeedupDeparture");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
		AccelerationManager am = AccelerationManager.getManager();
		am.currentScenarioIs(Scenario.DEPARTURE, 13.393, 11.0);
		double limit = am.getAccelLimit();
		assertEquals(0.9, limit, 0.0001);
	}
	
	@Test
	public void testSlowDownDeparture() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testSlowDownDeparture");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
		AccelerationManager am = AccelerationManager.getManager();
		am.currentScenarioIs(Scenario.DEPARTURE, 8.882, 11.0);
		double limit = am.getAccelLimit();
		assertEquals(1.83, limit, 0.0001);
	}

    @After
    public void shutdown() {
		try {
			LoggerManager.setOutputFile("logs/AccelerationManager.txt");
			LoggerManager.writeToDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	//////////////////
	// member elements
	//////////////////
	
    private AppConfig			config_;
	private static Logger		log_ = (Logger)LoggerManager.getLogger(AccelerationManagerTester.class);
}
