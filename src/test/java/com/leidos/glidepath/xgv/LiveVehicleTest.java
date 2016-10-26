package com.leidos.glidepath.xgv;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.xgv.messages.JausMessage;
import com.leidos.glidepath.xgv.messages.ReportDiscreteDevicesMessage;
import com.leidos.glidepath.xgv.messages.ReportStatusMessage;
import com.leidos.glidepath.xgv.messages.ReportVelocityMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertTrue;

/**
 * Designed to communicate to the XGV in a live vehicle test scenario. Not designed to be an automated test at build
 * time.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class LiveVehicleTest {

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
    }

    @Test
    public void testHeartbeat() {
        AppConfig appConfig = GlidepathApplicationContext.getInstance().getAppConfig();
        String xgvAddress = appConfig.getXgvIpAddress();
        InetSocketAddress outbound = new InetSocketAddress(xgvAddress, appConfig.getJausUdpPort());
        XgvConnection connection = null;
        try {
            connection = new XgvConnection(new DatagramSocket(appConfig.getJausUdpPort()), outbound);

            System.out.println("Waiting for heartbeat...");
            JausMessage heartbeat = connection.waitForMessage(JausMessage.JausCommandCode.HEARTBEAT);
            assertTrue("Failed to detect device presence!", heartbeat != null);
            System.out.println(heartbeat);
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    @Test
    public void testComponentStatus() {
        AppConfig appConfig = GlidepathApplicationContext.getInstance().getAppConfig();
        String xgvAddress = appConfig.getXgvIpAddress();
        InetSocketAddress outbound = new InetSocketAddress(xgvAddress, appConfig.getJausUdpPort());
        int mpdId = appConfig.getMpdJausId();
        XgvConnection connection = null;
        try {
            connection = new XgvConnection(new DatagramSocket(appConfig.getJausUdpPort()), outbound);

            System.out.println("Waiting for heartbeat...");
            JausMessage heartbeat = connection.waitForMessage(JausMessage.JausCommandCode.HEARTBEAT);
            assertTrue("Failed to detect device presence!", heartbeat != null);
            System.out.println(heartbeat);

            System.out.println("Querying motion profile driver component status...");
            connection.sendComponentStatusQueryMessage(mpdId);
            ReportStatusMessage mpdStatus = new ReportStatusMessage(connection.waitForMessage(JausMessage.JausCommandCode.REPORT_COMPONENT_STATUS));
            assertTrue("Failed to retrieve motion profile driver status!", mpdStatus != null);

            System.out.println(mpdStatus);
            System.out.println("MPD Status Variables:" +
                                "\nManual Override: " + mpdStatus.isManualOverrideEngaged() +
                                "\nSafe stop link status: " + mpdStatus.getSafeStopLinkStatus());

            connection.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    @Test
    public void testVelocityQuery() {
        AppConfig appConfig = GlidepathApplicationContext.getInstance().getAppConfig();
        String xgvAddress = appConfig.getXgvIpAddress();
        InetSocketAddress outbound = new InetSocketAddress(xgvAddress, appConfig.getJausUdpPort());
        XgvConnection connection = null;
        try {
            connection = new XgvConnection(new DatagramSocket(appConfig.getJausUdpPort()), outbound);

            System.out.println("Waiting for heartbeat...");
            JausMessage heartbeat = connection.waitForMessage(JausMessage.JausCommandCode.HEARTBEAT);
            assertTrue("Failed to detect device presence!", heartbeat != null);
            System.out.println(heartbeat);

            System.out.println("Querying velocity state...");
            connection.sendQueryVelocityStateMessage();
            ReportVelocityMessage velocityState = new ReportVelocityMessage(connection.waitForMessage(JausMessage.JausCommandCode.REPORT_VELOCITY_STATE));
            assertTrue("Failed to retrieve velocity state!", velocityState != null);
            System.out.println("VSS reported velocity = " + velocityState.getVelocity());

            connection.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    @Test
    public void testDiscreteDevicesQuery() {
        AppConfig appConfig = GlidepathApplicationContext.getInstance().getAppConfig();
        String xgvAddress = appConfig.getXgvIpAddress();
        InetSocketAddress outbound = new InetSocketAddress(xgvAddress, appConfig.getJausUdpPort());
        XgvConnection connection = null;
        try {
            connection = new XgvConnection(new DatagramSocket(appConfig.getJausUdpPort()), outbound);

            System.out.println("Waiting for heartbeat...");
            JausMessage heartbeat = connection.waitForMessage(JausMessage.JausCommandCode.HEARTBEAT);
            assertTrue("Failed to detect device presence!", heartbeat != null);
            System.out.println(heartbeat);

            System.out.println("Querying discrete devices...");
            connection.sendDiscreteDeviceStateQuery();
            ReportDiscreteDevicesMessage discreteDevicesMessage = new ReportDiscreteDevicesMessage(connection.waitForMessage(JausMessage.JausCommandCode.REPORT_VELOCITY_STATE));
            assertTrue("Failed to retrieve discrete device status!", discreteDevicesMessage != null);
            System.out.println("Discrete devices reported gear = " + discreteDevicesMessage.getGear() +
                    "\nDiscrete devices reported parking brake = " + discreteDevicesMessage.isParkingBrakeSet());

            connection.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

}
