package com.leidos.glidepath.xgv;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.xgv.messages.JausMessage;
import com.leidos.glidepath.xgv.simulated.SimulatedHeartbeatServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.*;

import static org.junit.Assert.assertTrue;

/**
 * Tests the XgvConnection against simulated XGV servers.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class SimXgvTester {

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
    }


    @Test
    public void testSimulatedHeartbeat() {
        try {
            SimulatedHeartbeatServer heartbeatServer = new SimulatedHeartbeatServer(9999, new InetSocketAddress("localhost", 3794));
            XgvConnection connection = new XgvConnection(new DatagramSocket(3794), new InetSocketAddress("localhost", 9999));

            Thread serverThread = new Thread(heartbeatServer);
            serverThread.start();
            JausMessage heartbeat = connection.waitForMessage(JausMessage.JausCommandCode.HEARTBEAT);
            heartbeatServer.stop();
            assertTrue(heartbeat != null);
            System.out.println(heartbeat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
