package com.leidos.glidepath.ucr;

import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Broadcasts the necessary data packets to the UCR GUI application.
 */
public class UcrGuiServer {
    private DatagramSocket sock;
    private InetSocketAddress dest;
    private ILogger log;

    private int RPM = 4000;

    public UcrGuiServer() throws SocketException {
        AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
        log = LoggerManager.getLogger(getClass());
        sock = new DatagramSocket(config.getUcrPort() + 1);
        dest = new InetSocketAddress(config.getUcrIpAddress(), config.getUcrPort());
    }

    public void send(DataElementHolder data) throws IOException {
        String packet = serialize(data);
        DatagramPacket p = new DatagramPacket(packet.getBytes(), packet.getBytes().length, dest);
        sock.send(p);
        log.infof("", "Sent \"%s\" to UCR GUI.", packet);
    }

    public void close() {
        sock.close();
    }

    private String serialize(DataElementHolder data) {
        int spatPhase = 0;
        if (data.get(DataElementKey.SIGNAL_PHASE) != null) {
            spatPhase = ((PhaseDataElement) data.get(DataElementKey.SIGNAL_PHASE)).value().value();
        }
        double spatTimeMin = data.getDoubleElement(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE);
        double curSpeed = data.getDoubleElement(DataElementKey.SPEED) * Constants.MPS_TO_MPH;
        double targetSpeed = data.getDoubleElement(DataElementKey.SPEED_COMMAND) * Constants.MPS_TO_MPH;
        double distanceToStopBar = data.getDoubleElement(DataElementKey.DIST_TO_STOP_BAR);
        return String.format("p=%d,n=%f,s=%f,m=%d,r=%f,d=%f,",
                            spatPhase,
                            spatTimeMin,
                            curSpeed,
                            RPM,
                            targetSpeed,
                            distanceToStopBar);
    }
}
