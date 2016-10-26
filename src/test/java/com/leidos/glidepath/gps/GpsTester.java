package com.leidos.glidepath.gps;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.gps.simulated.SimulatedPinpointDevice;
import org.apache.log4j.BasicConfigurator;
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

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class GpsTester {

    private SimulatedPinpointDevice pinpointDevice;
    private GpsConsumer gpsConsumer;

    private static ExecutorService executorService = null;

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
        pinpointDevice = new SimulatedPinpointDevice(GlidepathApplicationContext.getInstance().getAppConfig().getGpsPort());
        gpsConsumer = new GpsConsumer();
        gpsConsumer.setServerIp("localhost");       // ignore configured value
    }


    @Test
    /**
     * Test to read lat/long from simulated device
     * Right now, server is just sending lat=1.0, long=2.0 and incrementing each call
     */
    public void testNio() throws Exception {

        new Thread(pinpointDevice).start();

        // consumer start just sets up, must grab and run initializer
        gpsConsumer.initialize();

        Callable<Boolean> initializer = gpsConsumer.getInitializer();

        executorService = Executors.newFixedThreadPool(2);

        Future<Boolean> initFuture = executorService.submit(initializer);
        Boolean isInitialized = initFuture.get();
        assertTrue(isInitialized);

        for (int i=0; i<50; i++)   {
            Future<Boolean> serverFuture = executorService.submit((Callable<Boolean>)pinpointDevice);
            Future<DataElementHolder> clientFuture = executorService.submit(gpsConsumer);
            Boolean serverResult = serverFuture.get();
            DataElementHolder holder = clientFuture.get();

            //System.out.println("getGlobaPose info: " + holder.getLatitude() + " : " + holder.getLongitude());

            // 38.957198, -77.145701
            if (i == 0)   {
                assertTrue((float) holder.getLatitude() == 38.9572f);
                assertTrue((float) holder.getLongitude() == -77.1457f);
            }

            Thread.sleep(10);
        }

        executorService.shutdown();

        gpsConsumer.terminate();
        pinpointDevice.stop();
    }


    /**
     * Can't connect to device
     */
    @Test(expected=Exception.class)
    public void noServer() throws Exception {

        // consumer start just sets up, must grab and run initializer
        gpsConsumer.terminate();

        Callable<Boolean> initializer = gpsConsumer.getInitializer();

        executorService = Executors.newFixedThreadPool(2);

        // as server is not running, this should throw an exception
        Future<Boolean> initFuture = executorService.submit(initializer);
        Boolean isInitialized = initFuture.get();

    }

}
