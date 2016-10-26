package com.leidos.glidepath.dvi.services;

import com.leidos.glidepath.IConsumerTask;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.SpeedControl;
import com.leidos.glidepath.dvi.simulated.testconsumers.TestAsdConsumer;
import com.leidos.glidepath.dvi.simulated.testconsumers.TestGpsConsumer;
import com.leidos.glidepath.dvi.simulated.testconsumers.TestXgvConsumer;
import com.leidos.glidepath.gps.GpsConsumer;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
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

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = { DviExecutorService.class, AppConfig.class, SimpMessagingTemplate.class, AbstractMessageChannel.class } )
@SpringApplicationConfiguration(classes = SpeedControl.class)
@WebAppConfiguration
public class ExecutorTester {
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

    @Ignore
    public void testGpsCantConnect()   {
        boolean startConsume = dviExecutorService.getAutoStartConsumption();

        // we can override loading all consumers by directly setting consumer list in the service
        List<IConsumerTask> consumers = new ArrayList<IConsumerTask>();

        IConsumerTask consumer = new GpsConsumer();
        consumers.add(consumer);

        dviExecutorService.setConsumers(consumers);

        boolean result = dviExecutorService.start();

        // gps initializer fail because we cannot connect as no pinpoint
        assertFalse(result);

        try  {
            Thread.sleep(2000);
        }
        catch(Exception e)   {

        }

        dviExecutorService.stop();

    }


    @Ignore
    public void testFakeGps()   {
        // we can override loading all consumers by directly setting consumer list in the service
        List<IConsumerTask> consumers = new ArrayList<IConsumerTask>();

        IConsumerTask consumer = new TestGpsConsumer();
        consumers.add(consumer);

        dviExecutorService.setConsumers(consumers);

        boolean result = dviExecutorService.start();

        // test gps initializer should return true
        assertTrue(result);

        try  {
            Thread.sleep(2000);
        }
        catch(Exception e)   {

        }

        dviExecutorService.stop();

    }


    @Test
    public void testAllTestConsumers()   {
        boolean startConsume = dviExecutorService.getAutoStartConsumption();

        // we can override loading all consumers by directly setting consumer list in the service
        List<IConsumerTask> consumers = new ArrayList<IConsumerTask>();

        IConsumerTask consumer = new TestAsdConsumer();
        consumers.add(consumer);

        IConsumerTask gpsConsumer = new TestGpsConsumer();
        consumers.add(gpsConsumer);

        IConsumerTask xgvConsumer = new TestXgvConsumer();
        consumers.add(xgvConsumer);

        dviExecutorService.setConsumers(consumers);

        boolean result = dviExecutorService.start();

        // gps initializer fail because we cannot connect as no pinpoint
        assertTrue(result);

        try  {
            Thread.sleep(4000);
        }
        catch(Exception e)   {

        }

        dviExecutorService.stop();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
