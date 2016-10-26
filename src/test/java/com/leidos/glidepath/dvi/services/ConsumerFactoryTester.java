package com.leidos.glidepath.dvi.services;

import com.leidos.glidepath.IConsumerTask;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class ConsumerFactoryTester {
    @Autowired
    ApplicationContext applicationContext;

    AppConfig appConfig;

    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
        appConfig = GlidepathApplicationContext.getInstance().getAppConfig();
    }

    @Test
    public void test1()   {
        String consumerClazz = appConfig.getProperty("consumer.0");
        assertTrue(consumerClazz != null);
        assertTrue(consumerClazz.length() > 0);

        consumerClazz = appConfig.getProperty("consumer.X");
        assertTrue(consumerClazz == null);

    }

    @Ignore
    public void getConsumer()   {
        IConsumerTask consumer = ConsumerFactory.getConsumer("com.leidos.glidepath.gps.GpsConsumer");

        assertTrue(consumer instanceof IConsumerTask);
    }

    @Test
    public void getConsumers()   {
        List<IConsumerTask> consumers = ConsumerFactory.getConsumers();
        assertTrue(consumers.size() > 0);

    }
}
