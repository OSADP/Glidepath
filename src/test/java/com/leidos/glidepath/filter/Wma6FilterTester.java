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
import com.leidos.glidepath.filter.IDataFilter;
import com.leidos.glidepath.filter.Wma6Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class Wma6FilterTester {
	
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
    public void testOneValue() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testOneValue");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	Wma6Filter s = new Wma6Filter();
    	s.initialize(0.1);

    	s.addRawDataPoint(64.312);
    	double v = s.getSmoothedValue();
    	assertEquals(64.312, v, 0.0001);
    }
    
    @Test
    public void testTwoValues() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testTwoValues");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	Wma6Filter s = new Wma6Filter();
    	s.initialize(0.1);

    	s.addRawDataPoint(64.312);
    	double v1 = s.getSmoothedValue();
    	assertEquals(64.312, v1, 0.0001);
    	
    	s.addRawDataPoint(58.9);
    	double v2 = s.getSmoothedValue();
    	assertEquals(61.36, v2, 0.0001);
    }
    
    @Test
    public void testSixValues() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testSixValues");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	Wma6Filter s = new Wma6Filter();
    	s.initialize(0.1);
    	double v;
    	v = getSmoothedValue(s, 14.0);
    	v = getSmoothedValue(s, 15.0);
    	v = getSmoothedValue(s, 16.0);
    	v = getSmoothedValue(s, 19.0);
    	v = getSmoothedValue(s, 18.0);
    	v = getSmoothedValue(s, 19.0);
    	assertEquals(17.7143, v, 0.0001);
    }
    
    @Test
    public void test8Values() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering test8Values");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	Wma6Filter s = new Wma6Filter();
    	s.initialize(0.1);
    	double v;
    	v = getSmoothedValue(s, 14.0);
    	v = getSmoothedValue(s, 15.0);
    	v = getSmoothedValue(s, 16.0);
    	v = getSmoothedValue(s, 19.0);
    	v = getSmoothedValue(s, 18.0);
    	v = getSmoothedValue(s, 19.0);
    	v = getSmoothedValue(s, 19.5);
    	v = getSmoothedValue(s, 21.3);
    	assertEquals(19.4905, v, 0.0001);
    }
    
    @Test
    public void test13Values() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering test13Values");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	Wma6Filter s = new Wma6Filter();
    	s.initialize(0.1);
    	double v;
    	v = getSmoothedValue(s, 14.0);
    	v = getSmoothedValue(s, 15.0);
    	v = getSmoothedValue(s, 16.0);
    	v = getSmoothedValue(s, 19.0);
    	v = getSmoothedValue(s, 18.0);
    	v = getSmoothedValue(s, 19.0);
    	v = getSmoothedValue(s, 19.5);
    	v = getSmoothedValue(s, 21.3);
    	v = getSmoothedValue(s, 21.7);
    	v = getSmoothedValue(s, 22.0);
    	v = getSmoothedValue(s, 23.1);
    	v = getSmoothedValue(s, 24.2);
    	v = getSmoothedValue(s, 25.0);
    	assertEquals(23.5286, v, 0.0001);
    }
    
    @After
    public void shutdown() {
		try {
			LoggerManager.setOutputFile("logs/Wma6Filter.txt");
			LoggerManager.writeToDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    //////////////////
    // private members
    //////////////////
    
    private double getSmoothedValue(IDataFilter s, double val) {
    	s.addRawDataPoint(val);
    	return s.getSmoothedValue();
    }
    
    private AppConfig			config_;
	private static Logger		log_ = (Logger)LoggerManager.getLogger(Wma6FilterTester.class);
}
