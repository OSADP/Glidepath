package com.leidos.glidepath.xgv;

import com.leidos.glidepath.IConsumerInitializer;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import com.leidos.glidepath.xgv.messages.JausMessage;
import com.leidos.glidepath.xgv.messages.JausMessage.JausCommandCode;

import java.net.*;

/**
 * Initializes a connection with the XGV.
 *
 * Sets up UDP sockets to send and receive JUDP packets on the GlidePath vehile network.
 * Then waits for a heartbeat pulse to be received from the XGV device to verify that it is listening on the correct
 * port. After a successful initialization (indicated by a return value of true), the XgvConnection should be retrieved
 * using the getConnection() method.
 */
public class XgvInitializer implements IConsumerInitializer {
    private ILogger log;
    private XgvConnection connection = null;

    public XgvInitializer(XgvConnection connection) {
        this.connection = connection;
        log = LoggerManager.getLogger(this.getClass());
        log.debugf("XGV", "XgvInitializer created with connection = %s", connection == null ? "NULL" : "valid");
    }

    @Override
    public Boolean call() throws Exception {
        AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
        // TODO: Cleanup error handling logic/flow for this function
        try {
            // Attempt to capture heartbeat packet
            connection.setTimeout(config.getXgvInitTimeout());
            JausMessage heartbeat = connection.waitForMessage(JausCommandCode.HEARTBEAT);

            // Failed to communicate to XGV
            if (heartbeat == null) {
                log.error("!!!", "Failed to communicate with the XGV after 10 retries.");
                return false;
            } else {
                log.info("", "Successfully received heartbeat packet from XGV!");
            }

            // TODO: Ensure the components we want to talk to are online

        } catch (SocketException e) {
            log.caughtExcept("!!!", "Error opening XGVConnection.", e);
            return false;
        }

        // Set the XGV connection timeout to normal operation value
        connection.setTimeout(config.getXgvSocketTimeout());
        return true;
    }
}
