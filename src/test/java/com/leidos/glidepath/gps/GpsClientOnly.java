package com.leidos.glidepath.gps;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.gps.simulated.SimulatedPinpointDevice;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import org.apache.log4j.BasicConfigurator;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class GpsClientOnly {

    private static ILogger logger = LoggerManager.getLogger(GpsClientOnly.class);

    private GpsConsumer gpsConsumer;

    private ExecutorService executorService = null;

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
        LoggerManager.setOutputFile("logs/speedcontrol.log");
        gpsConsumer = new GpsConsumer();
    }


    @Test
    /**
     * Test to read lat/long from simulated device
     * Right now, server is just sending lat=1.0, long=2.0 and incrementing each call
     */
    public void testNio() throws Exception {
        // consumer start just sets up, must grab and run initializer
        gpsConsumer.initialize();

        Callable<Boolean> initializer = gpsConsumer.getInitializer();

        executorService = Executors.newFixedThreadPool(2);

        Future<Boolean> initFuture = executorService.submit(initializer);

        Boolean isInitialized = false;

        try   {
            isInitialized = initFuture.get();
        }
        catch(Exception e)   {
            logger.error("GPS", "Error initializing GpsConsumer: " + e.getMessage());
            try  {
                LoggerManager.writeToDisk();
            }
            catch(Exception e2) {};
        }

        if (isInitialized)   {
            for (int i=0; i<50; i++)   {
                DateTime startTime = new DateTime();
                Future<DataElementHolder> clientFuture = executorService.submit(gpsConsumer);
                DataElementHolder holder = clientFuture.get();
                DateTime endTime = new DateTime();
                Duration duration = new Duration(startTime, endTime);
                logger.debug("GPS", "GpsConsumer cycle: " + duration.getMillis() + "ms");

                try  {
                    LoggerManager.writeToDisk();
                }
                catch(Exception e2) {};

                //System.out.println("getGlobaPose info: " + holder.getLatitude() + " : " + holder.getLongitude());
                Thread.sleep(10);
            }
        }

        gpsConsumer.terminate();

        executorService.shutdown();
    }

}
