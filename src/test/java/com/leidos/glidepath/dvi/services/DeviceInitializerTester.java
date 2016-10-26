package com.leidos.glidepath.dvi.services;

import com.leidos.glidepath.IConsumerTask;
import com.leidos.glidepath.IProducerTask;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.dvi.simulated.testconsumers.TestCanConsumer;
import com.leidos.glidepath.dvi.simulated.testconsumers.TestExceptionConsumerInitializer;
import com.leidos.glidepath.dvi.simulated.testconsumers.TestFailedConsumerInitializer;
import com.leidos.glidepath.dvi.simulated.testconsumers.TestGpsConsumer;
import com.leidos.glidepath.dvi.simulated.testconsumers.TestXgvConsumer;
import com.leidos.glidepath.gps.GpsConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class DeviceInitializerTester {

    @Autowired
    ApplicationContext applicationContext;

    private IConsumerTask gpsConsumer;
    private IConsumerTask canConsumer;


    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
    }


    @Test
    public void initializeNoConsumers()   {
        DeviceInitializer deviceInitializer = new DeviceInitializer();

        boolean result = deviceInitializer.start();
        assertFalse(result);
    }


    @Test
    public void initializeCanConsumer()   {
        canConsumer = new TestCanConsumer();
        DeviceInitializer deviceInitializer = new DeviceInitializer();

        deviceInitializer.addConsumer(canConsumer);

        boolean result = deviceInitializer.start();
        assertTrue(result);
    }

    @Test
    public void initializeGpsConsumer()   {
        gpsConsumer = new TestGpsConsumer();
        DeviceInitializer deviceInitializer = new DeviceInitializer();

        deviceInitializer.addConsumer(gpsConsumer);

        boolean result = deviceInitializer.start();
        assertTrue(result);

    }


    @Test
    public void initializeAllConsumers()   {
        gpsConsumer = new TestGpsConsumer();
        canConsumer = new TestCanConsumer();
        DeviceInitializer deviceInitializer = new DeviceInitializer();

        deviceInitializer.addConsumer(gpsConsumer);
        deviceInitializer.addConsumer(canConsumer);

        boolean result = deviceInitializer.start();
        assertTrue(result);

    }

    @Test
    public void initializeSingleFailedConsumer()   {
        gpsConsumer = new TestGpsConsumer(new TestFailedConsumerInitializer());

        DeviceInitializer deviceInitializer = new DeviceInitializer();

        deviceInitializer.addConsumer(gpsConsumer);

        boolean result = deviceInitializer.start();
        assertFalse(result);

    }

    //@Test
    // TODO: determine whether one fails, return false
    public void initializeMultipleConsumersWithOneFail()   {
        IConsumerTask failingConsumer = new TestGpsConsumer(new TestFailedConsumerInitializer());
        gpsConsumer = new TestGpsConsumer();
        canConsumer = new TestCanConsumer();

        DeviceInitializer deviceInitializer = new DeviceInitializer();

        deviceInitializer.addConsumer(gpsConsumer);
        deviceInitializer.addConsumer(canConsumer);
        deviceInitializer.addConsumer(failingConsumer);

        boolean result = deviceInitializer.start();
        assertFalse(result);

    }

    @Test
    public void initializeThrowsException()   {
        IConsumerTask exceptionConsumer = new TestGpsConsumer(new TestExceptionConsumerInitializer());

        assertFalse(exceptionConsumer instanceof IProducerTask);

        DeviceInitializer deviceInitializer = new DeviceInitializer();

        deviceInitializer.addConsumer(exceptionConsumer);

        boolean result = deviceInitializer.start();
        assertFalse(result);

    }


    @Test
    public void initializeXvg()   {
        IConsumerTask consumer = new TestXgvConsumer();

        assertTrue(consumer instanceof IProducerTask);

        DeviceInitializer deviceInitializer = new DeviceInitializer();

        deviceInitializer.addConsumer(consumer);

        boolean result = deviceInitializer.start();
        assertTrue(result);
    }

    @Test
    public void realGpsConsumerShouldFail()   {
        // TODO: modified factory for now to provide fake consumers
        //IConsumerTask consumer = ConsumerFactory.getConsumer(ConsumerFactory.ConsumerType.GPS_CONSUMER);
        IConsumerTask consumer = new GpsConsumer();
        ((GpsConsumer) consumer).setServerIp("localhost");

        assertFalse(consumer instanceof IProducerTask);

        DeviceInitializer deviceInitializer = new DeviceInitializer();

        deviceInitializer.addConsumer(consumer);

        boolean result = deviceInitializer.start();
        assertFalse(result);
    }

}
