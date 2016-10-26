package com.leidos.glidepath.appcommon.utils;

import com.leidos.glidepath.dvi.AppConfig;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Demonstrates how to use AppConfig in tests
 *
 * During normal running in non Spring managed classes, we can acquire using:
 *      AppConfig appConfig = GlidepathApplicationContext.getInstance().getAppConfig();
 *
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class GlidepathApplicationContextTester {

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
    }

    @Test
    public void testAppConfig()   {
        // our SpringApplication sets the context in our GlidepathApplicationContext singleton
        // we can get AppConfig or any Spring bean via this singleton
        AppConfig appConfig = GlidepathApplicationContext.getInstance().getAppConfig();
        assertTrue(appConfig.getPeriodicDelay() == 100);
    }

}
