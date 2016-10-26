package com.leidos.glidepath.can;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

public class CanConsumerTester {
    private CanConsumer canConsumer;

    private static ExecutorService executorService = null;
    private ILogger logger = LoggerManager.getLogger(CanConsumerTester.class);

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void before()   {
        LoggerManager.setOutputFile("logs/speedcontrol.log");
        logger.info("",  "");
        logger.info("====", "=============================");
        logger.info("CAN", "CanConsumerTester started.");
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
        canConsumer = new CanConsumer();

    }

    @Test
    public void testCan()   {
        // consumer start just sets up, must grab and run initializer
        canConsumer.initialize();

        Callable<Boolean> initializer = canConsumer.getInitializer();

        executorService = Executors.newFixedThreadPool(2);

        try   {
            Future<Boolean> initFuture = executorService.submit(initializer);
            Boolean isInitialized = initFuture.get();
            assertTrue(isInitialized);

            if (isInitialized)   {
                for (int i=0; i<50; i++)   {
                    Future<DataElementHolder> clientFuture = executorService.submit(canConsumer);
                    DataElementHolder holder = clientFuture.get();

                    // right now, holder is empty
                    assertTrue(holder.size() == 0);

                    Thread.sleep(10);
                }
            }
        }
        catch(Exception e)   {
            logger.caughtExcept("CAN", "Error looping in CanConsumerTester: ", e);
        }

        executorService.shutdown();

        canConsumer.terminate();

        try   {
            LoggerManager.writeToDisk();
        }
        catch(Exception e) {}
    }

}
