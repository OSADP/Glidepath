package com.leidos.glidepath.can;

import com.leidos.glidepath.IConsumerInitializer;
import com.leidos.glidepath.IConsumerTask;
import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.IOException;

/**
 * Skeleton CanConsumer
 */
public class CanConsumer implements IConsumerTask {

    private IConsumerInitializer canInitializer;
    private int retries = 10;
    CanSocket sock;
    ILogger logger = LoggerManager.getLogger(CanConsumer.class);

    @Override
    public boolean initialize() {
        AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
        sock = new CanSocket(config.getCanDeviceName());
        this.canInitializer = new CanConsumerInitializer(sock);
        return true;
    }

    @Override
    public void terminate() {
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public IConsumerInitializer getInitializer()   {
        return canInitializer;
    }

    /**
     * Periodic write/read of CAN data from serial port
     *
     * @return
     * @throws IOException
     */
    public DataElementHolder call() throws IOException {
        DataElementHolder holder = new DataElementHolder();

        byte[] data = new byte[8];
        CanFrame mafRequest = new CanFrame(CanFrame.CanId.MASS_AIRFLOW_REQUEST, 0, data);
        sock.sendCanFrame(mafRequest);

        CanFrame massAirFlow = null;
        for (int i = 0; i < retries; i++) {
            CanFrame in = sock.recvCanFrame();
            if (in.getId() == CanFrame.CanId.MASS_AIRFLOW_RESPONSE) {
                massAirFlow = in;
                break;
            }
        }

        if (massAirFlow != null) {
            // TODO: Save MAF data into DataElementHolder
        }

        return holder;
    }
}
