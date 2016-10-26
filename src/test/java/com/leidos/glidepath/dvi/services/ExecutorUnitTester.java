package com.leidos.glidepath.dvi.services;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.DataElementKey;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.SpeedControl;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import com.leidos.glidepath.dvi.simulated.testutils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpeedControl.class)
@WebAppConfiguration
public class ExecutorUnitTester {
    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    DviExecutorService dviExecutorService;

    ILogger logger;

    @Before
    public void before()   {
        LoggerManager.setOutputFile("logs/speedcontrol.log");
        logger = LoggerManager.getLogger(ExecutorTester.class);
        logger.info("", "");
        logger.info("====", "============================");
        logger.infof("TEST", this.getClass().getSimpleName() + " started.");
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
    }


    @Test
    public void testValidateFullHolder()   {
        DataElementHolder holder = TestUtils.createFullDataElementHolder();

        boolean result = holder.validate();
        assertTrue(result);

    }

    @Test
    public void testMissingOneValue()   {
        DataElementKey[] keys = DataElementKey.values();

        DataElementHolder holder = TestUtils.createDataElementHolder(DataElementKey.LATITUDE);

        boolean result = holder.validate();
        assertFalse(result);

    }


    @After
    public void after() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("TEST", "Test completed.");

        try {
            LoggerManager.writeToDisk();
        }catch (Exception e) {
            //do nothing for now
        }
    }


}
