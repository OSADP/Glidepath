package com.leidos.glidepath.filter;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.logger.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.filter.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class NoFilterTester {

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
    public void testAll() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testAll");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
		NoFilter s = new NoFilter();
		s.initialize(0.1);
		
		s.addRawDataPoint(23.0);
		s.addRawDataPoint(25.5);

		double speed = s.getSmoothedValue();
		assertEquals(25.5, speed, 0.0001);
		double accel = s.getSmoothedDerivative();
		assertEquals(25.0, accel, 0.001);
		
		s.addRawDataPoint(26.0);
		
		speed = s.getSmoothedValue();
		assertEquals(26.0, speed, 0.0001);
		
		accel = s.getSmoothedDerivative();
		assertEquals(5.0, accel, 0.001);
		
		double jerk = s.getSmoothedSecondDerivative();
		assertEquals(-200.0, jerk, 0.01);
		
		s.addRawDataPoint(25.9);
		speed = s.getSmoothedValue();
		assertEquals(25.9, speed, 0.0001);
		
		accel = s.getSmoothedDerivative();
		assertEquals(-1.0, accel, 0.0001);
		
		jerk = s.getSmoothedSecondDerivative();
		assertEquals(-60.0, jerk, 0.0001);
    }
    
    @After
    public void shutdown() {
		try {
			LoggerManager.setOutputFile("logs/NoFilter.txt");
			LoggerManager.writeToDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    //////////////////
    // private members
    //////////////////
    
    
    private AppConfig			config_;
	private static Logger		log_ = (Logger)LoggerManager.getLogger(NoFilterTester.class);
}
