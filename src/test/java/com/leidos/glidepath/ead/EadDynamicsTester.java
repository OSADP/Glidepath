package com.leidos.glidepath.ead;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.appcommon.Constants;
import com.leidos.glidepath.appcommon.SignalPhase;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.Logger;
import com.leidos.glidepath.logger.LoggerManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class EadDynamicsTester {

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
	public void testBasic() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering testBasic");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
    	//set up new object & initialize
		EadDynamics ead = new EadDynamics();
		ead.initialize(100, 0);
		
		double ts = 0.0;
		try {
			
			//start the speed excursions
			ead.setState(8.0, 8.0, 0.0, 150.0, SignalPhase.GREEN.value(), 25.0, 30.0);
			double ts1 = ead.getTargetSpeed();
			assertTrue(ts1 > ts);
			
			double s2 = 8.0+0.1*(ts1-8.0);
			double a2 = (s2 - 8.0)/0.1;
			ead.setState(s2, 8.0, a2, 150.0, SignalPhase.GREEN.value(), 25.0, 30.0);
			double ts2 = ead.getTargetSpeed();
			assertTrue(ts2 > ts1);

			double s3 = s2 + 1.0*(ts2-ts1);
			double a3 = (s3 - s2)/0.1;
			ead.setState(s3, 8.0, a3, 150.0, SignalPhase.GREEN.value(), 25.0, 30.0);
			double ts3 = ead.getTargetSpeed();
			assertTrue(ts3 > ts2); //we're overshooting already

		
		} catch (Exception e) {
			log_.error("TEST", "Exception thrown from setState: " + e.toString());
			assertTrue(false);
		}
	}
    
    @After
    public void shutdown() {
		try {
			LoggerManager.setOutputFile("logs/EadDynamics.txt");
			LoggerManager.writeToDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    //////////////////
    // private members
    //////////////////
    
    private AppConfig			config_;
	private static Logger		log_ = (Logger)LoggerManager.getLogger(EadDynamicsTester.class);
}
