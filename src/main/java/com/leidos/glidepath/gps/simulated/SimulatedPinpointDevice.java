package com.leidos.glidepath.gps.simulated;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.gps.NioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;


public class SimulatedPinpointDevice implements Runnable, Callable<Boolean> {

    private static Logger logger = LoggerFactory.getLogger(SimulatedPinpointDevice.class);

    private ServerSocketChannel serverSocketChannel;
    private SocketChannel socketChannel;
    private DatagramChannel datagramChannel;
    private DatagramChannel clientDatagramChannel;
    private NioUtils nioUtils;
    private SocketAddress clientUdpSocketAddress;
    private int listenPort;

    private static final String gpsFile = "testdata/gpsGlobalPositions.csv";

    private SimulatedGpsProducer producer = new SimulatedGpsProducer(gpsFile);

    private final static byte[] protocolVersion = { 0x41 };
    private final static byte[] setApi = { (byte) 0x89, 0x01, 0x01 };
    private final static byte[] setApiPayload = { 0x22 };

    private final static byte[] getUdpPort = { (byte) 0x90, 0x05 };
    private final static byte[] getUdpPortResponse = { (byte) 0x81, 0x05, 0x02, 0, 0};
    private final static int udpPort = 8321;

    // 11 byte UDP Ping...server sends 0x91, client responds with 0x81 and echos server data
    private final static byte[] UDP_PING_PACKET = { (byte) 0x91, 0x06, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09 };

    private byte[] GLOBAL_POSE_RESPONSE = { (byte) 0x80, 0x06, 0x1A,    // control, id, size (26)
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,     // Timestamp
            0x00, 0x00, 0x00, 0x01,          // LAT
            0x00, 0x00, 0x00, 0x02,          // LONG
            0x07, 0x08, 0x09, 0x0A,          // Altitude
            0x01, 0x02,                      // Roll
            0x03, 0x04,                      // Pitch
            0x05, 0x06    };                 // Yaw

    private AtomicBoolean shutdown = new AtomicBoolean(false);


    public SimulatedPinpointDevice(int listenPort)   {
        this.listenPort = listenPort;
        producer.load();
    }

    public void run() {
        try  {
            start();
        }
        catch(Exception e)   {
            logger.info("Server error starting: " + e.getMessage());
        }
    }

    public void start() throws Exception  {
        serverSocketChannel = ServerSocketChannel.open();

        serverSocketChannel.socket().bind(new InetSocketAddress(listenPort));
        serverSocketChannel.configureBlocking(true);

        socketChannel = serverSocketChannel.accept();

        if(socketChannel != null)  {
            nioUtils = new NioUtils(socketChannel);
            //do something with socketChannel...
            logger.info("Server accepted client connection.");
            initiateHandshake();
            //break;
        }

    }


    public Boolean call() throws Exception  {
        Boolean udpSendResult = sendUdpPing();
        Boolean tcpResult = processGetGlobalPose();
        Boolean udpReceiveResult = receiveUdpPing();

        return new Boolean(udpSendResult && tcpResult && udpReceiveResult);
    }


    /**
     * Read the getGlobalPose request from client and fill in lat/long and send response
     *
     * @return
     * @throws Exception
     */
    public Boolean processGetGlobalPose() throws Exception  {


        byte[] methodCall = nioUtils.receiveBytes(2);

        // 4th byte is method #
        if (methodCall[1] == 0x07)   {
            logger.info("Server received getGlobalPose call.");
        }

        // get data from our csv producer
        DataElementHolder holder = producer.getGpsData();
        logger.info("PRODUCER: " + holder.getLatitude() + " : " + holder.getLongitude());
        logger.info("PRODUCER (Float): " + (float) holder.getLatitude() + " : " + (float) holder.getLongitude());

        nioUtils.encodeLatLong(holder.getLatitude(), GLOBAL_POSE_RESPONSE, 11);
        nioUtils.encodeLatLong(holder.getLongitude(), GLOBAL_POSE_RESPONSE, 15);

        nioUtils.sendBytes(GLOBAL_POSE_RESPONSE);
        logger.info("Server sent getGlobalPose method call response.");

        return new Boolean(true);

    }

    /**
     * Pinpoint send UDP Ping to client
     *
     * @return
     * @throws Exception
     */
    private Boolean sendUdpPing() throws Exception   {
        // do the periodic send and read
        ByteBuffer buffer = ByteBuffer.allocate(11);
        buffer.put(UDP_PING_PACKET);
        buffer.flip();

        clientDatagramChannel = DatagramChannel.open();
        clientDatagramChannel.send(buffer, clientUdpSocketAddress);

        logger.info("Server send UDP Ping.");
        return new Boolean(true);

    }

    /**
     * Pinpoint receive UDP Ping response from client
     *
     * @return
     * @throws Exception
     */
    private Boolean receiveUdpPing() throws Exception   {
        ByteBuffer udpBuffer = ByteBuffer.allocate(11);
        byte[] clientPingResponse = new byte[11];

        clientDatagramChannel.receive(udpBuffer);

        udpBuffer.flip();
        udpBuffer.get(clientPingResponse, 0, 11);

        if (clientPingResponse[0] == (byte) 0x81)   {
            logger.info("Server received client UDP Ping response.");
        }

        return new Boolean(true);

    }


    public void stop() throws Exception   {
        shutdown.set(true);
        socketChannel.close();
        serverSocketChannel.close();
        datagramChannel.socket().close();
        datagramChannel.close();
        clientDatagramChannel.socket().close();
        clientDatagramChannel.close();

        logger.info("Shutting down server.");
    }


    public void initiateHandshake() throws Exception   {

        nioUtils.sendBytes(protocolVersion);

        byte serverProtocolVersion = nioUtils.receiveByte();

        nioUtils.sendBytes(setApi);
        nioUtils.sendBytes(setApiPayload);

        // get the client setApi data
        nioUtils.receiveBytes(6);

        // ask client for UDP port
        nioUtils.sendBytes(getUdpPort);

        byte[] clientUdpPortResponse = nioUtils.receiveBytes(5);

        // client port is indices 3 and 4
        short clientUdpPort = ByteBuffer.wrap(clientUdpPortResponse).order(ByteOrder.LITTLE_ENDIAN).getShort(3);
        logger.info("Client UDP port: " + clientUdpPort);

        // although server opens a UDP server and sends to client, it is never used
        // we use the clients port
        clientDatagramChannel = DatagramChannel.open();

        // save the client UDP port to send the pinpoint UDP Ping
        clientUdpSocketAddress = new InetSocketAddress("localhost", clientUdpPort);


        // now, read in client getUdpPort request
        byte[] clientUdpPortRequest = nioUtils.receiveBytes(2);
        datagramChannel = DatagramChannel.open();
        InetSocketAddress isa = new InetSocketAddress(udpPort);
        datagramChannel.socket().bind(isa);
        logger.info("Server setup UDP server on port: " + udpPort);

        if (clientUdpPortRequest[0] == (byte) 0x90 && clientUdpPortRequest[1] == 0x05)   {
            logger.info("Received GetUdpPort request from client.");

            byte[] localUdpPortArray = ByteBuffer.allocate(2).putShort((short) udpPort).order(ByteOrder.LITTLE_ENDIAN).array();
            getUdpPortResponse[3] = localUdpPortArray[0];
            getUdpPortResponse[4] = localUdpPortArray[1];

            nioUtils.sendBytes(getUdpPortResponse);
            logger.info("Server sent response to client's getUdpPort.");
        }

    }



}
