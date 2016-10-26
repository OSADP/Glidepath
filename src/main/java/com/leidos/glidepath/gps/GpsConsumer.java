package com.leidos.glidepath.gps;

import com.leidos.glidepath.IConsumerInitializer;
import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.IConsumerTask;
import com.leidos.glidepath.appcommon.DataElementKey;
import com.leidos.glidepath.appcommon.IntDataElement;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * GpsConsumer reads latitude and longitude from the Pinpoint device
 *
 */
public class GpsConsumer implements IConsumerTask {

    private static ILogger logger = LoggerManager.getLogger(GpsConsumer.class);

    private SocketChannel socketChannel;
    private DatagramChannel datagramChannel;
    private IConsumerInitializer gpsInitializer;
    private NioUtils nioUtils;
    private String serverIp;
    private int serverTcpPort;
    private int clientUdpPort;

    // size of the UDP Ping message. we echo the server packet, replacing the first character 0x91 with 0x81
    private final static int SIZE_UDP_PING = 11;

    // size of server getGlobalPose result
    private final static int SIZE_GET_GLOBAL_POSE = 29;

    // client packet sent to server to request global pose
    private final static byte[] GET_GLOBAL_POSE = { 0x10, 0x07 };

    /**
     * Index into getGlobalPose response of latitude, 4 bytes
     */
    private final static int INDEX_LATITUDE = 11;

    /**
     * Index into getGlobalPose response of longitude, 4 bytes
     */
    private final static int INDEX_LONGITUDE = 15;

    // size of server getVelocityState result
    private final static int SIZE_GET_VELOCITY_STATE = 29;

    // client packet sent to server to request velocity state
    private final static byte[] GET_VELOCITY_STATE = { 0x10, 0x0B };

    /**
     * Index into getVelocityState response of forward velocity, 3 bytes
     */
    private final static int INDEX_VELOCITY = 11;

    /**
     * Constructor sets ip/port info from appConfig in dvi.properties
     */
    public GpsConsumer()   {
        this.serverIp = GlidepathApplicationContext.getInstance().getAppConfig().getGpsHost();
        this.serverTcpPort = GlidepathApplicationContext.getInstance().getAppConfig().getGpsPort();
        this.clientUdpPort = GlidepathApplicationContext.getInstance().getAppConfig().getGpsUdpPort();
    }

    /**
     * The periodic Callable method that reads GPS data from the Pinpoint device
     * It first responds to the Pinpoint UDP ping and then uses the getGlobalPose method call to acquire
     * Latitude and Longitude
     *
     * @return
     * @throws Exception
     */
    public DataElementHolder call() throws IOException {
        DateTime now = new DateTime();
        DataElementHolder holder = getGlobalPose();
        Boolean udpResult = respondToUdpPing();

        Duration duration = new Duration(now, new DateTime());
        holder.put(DataElementKey.CYCLE_GPS, new IntDataElement((int) duration.getMillis()));

        return holder;
    }

    public IConsumerInitializer getInitializer()   {
        return gpsInitializer;
    }

    /**
     * Start the GPS client.  This opens TCP and UDP channels to the Pinpoint device and constructs an
     * Initializer for the Pinpoint Localization server.
     *
     * @return  true if successful initialization
     */
    public boolean initialize()  {

        Boolean bResult = false;
        ExecutorService executorService = null;

        try   {
            datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            socketChannel = SocketChannel.open();
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            nioUtils = new NioUtils(socketChannel);
            this.gpsInitializer = new GpsConsumerInitializer(serverIp, serverTcpPort, clientUdpPort, socketChannel, datagramChannel);

            bResult = true;
        }
        catch(Exception e)   {
            logger.error(ILogger.TAG_GPS, "Error starting GpsConsumer.", e);
        }
        finally   {
            if (!bResult)   {
                terminate();
            }
        }

        return bResult.booleanValue();
    }


    // U64  Microseconds since 1970
    // I32  Latitude (180 / 2^31 )
    // I32  Logitude (180 / 2^31 )
    // I32  HAE Altitude (mm)
    // I16  Roll (180 /2^15 )
    // I16  Pitch (180 /2^15 )
    // I16  Yaw (180 /2^15 )

    /**
     * Helper method to retrieve lat/long from the Pinpoint device using getGlobalPose method call
     *
     *
     * @return
     * @throws Exception
     */
    private DataElementHolder getGlobalPose() throws IOException  {
        DataElementHolder holder = new DataElementHolder();

        ByteBuffer sendBuffer = ByteBuffer.wrap(GET_GLOBAL_POSE);

        int bytesWritten = 0;
        try  {
            socketChannel.write(sendBuffer);
            logger.debug(ILogger.TAG_GPS, "Client sent getGlobalPose method call.");
        }
        catch(IOException ioe)   {
            logger.error(ILogger.TAG_GPS, "Error sending getGlobalPose:", ioe);
            return holder;
        }

        ByteBuffer receiveBuffer = ByteBuffer.allocate(SIZE_GET_GLOBAL_POSE);       // control, id, size, payload
        byte[] globalPose = new byte[SIZE_GET_GLOBAL_POSE];

        int bytesRead = socketChannel.read(receiveBuffer);

        if (bytesRead == SIZE_GET_GLOBAL_POSE)   {
            receiveBuffer.flip();
            receiveBuffer.get(globalPose);
            //logger.debug(ILogger.TAG_GPS, "Client received getGlobalPose result of " + globalPose.length + " bytes.");

            double latitude = nioUtils.decodeLatLong(globalPose, INDEX_LATITUDE);
            double longitude = nioUtils.decodeLatLong(globalPose, INDEX_LONGITUDE);

            holder.setLatitude(latitude);
            holder.setLongitude(longitude);

            //logger.debug(ILogger.TAG_GPS, "Retrieved Lat and Long: " + latitude + " : " + longitude);
        }

        return holder;

    }

    // TODO: If we end up getting velocity from Pinpoint, refactor into send and receive command

    /**
     * Sends getVelocityState API call to pinpoint and processes response
     *
     * @return  DataElementHolder containing SPEED element
     * @throws IOException
     */
    private DataElementHolder getVelocityState() throws IOException  {
        DataElementHolder holder = new DataElementHolder();

        ByteBuffer sendBuffer = ByteBuffer.wrap(GET_VELOCITY_STATE);

        int bytesWritten = 0;
        try  {
            socketChannel.write(sendBuffer);
            logger.debug(ILogger.TAG_GPS, "Client sent getVelocityState method call.");
        }
        catch(IOException ioe)   {
            logger.error(ILogger.TAG_GPS, "Error sending getVelocityState:", ioe);
            return holder;
        }

        ByteBuffer receiveBuffer = ByteBuffer.allocate(SIZE_GET_VELOCITY_STATE);       // control, id, size, payload
        byte[] velocityState = new byte[SIZE_GET_VELOCITY_STATE];

        int bytesRead = socketChannel.read(receiveBuffer);

        if (bytesRead == SIZE_GET_VELOCITY_STATE)   {
            receiveBuffer.flip();
            receiveBuffer.get(velocityState);
            logger.debug(ILogger.TAG_GPS, "Client received getVelocityState result of " + velocityState.length + " bytes.");

            double velocity = nioUtils.decodeVelocity(velocityState, INDEX_VELOCITY);

            holder.setSpeed(velocity);

            logger.debug(ILogger.TAG_GPS, "Retrieved Velocity from Pinpoint: " + velocity );
        }

        return holder;

    }


    /**
     * Responds to the UDP Ping from the Pinpoint Device
     * Server will wait up to 30 seconds for a response, so we can just do this a part of our read GPS cycle.
     * 11 byte UDP Ping...server sends 0x91, client responds with 0x81 and echos server data
     *
     * NOTE: When we connected to actual device, it was observed that the Pinpoint sent the UDP Ping approx 1 second
     * after receiving our client's ping response. Since we were blocking on the UDP channel, this resulted in only
     * reading GPS data every second as we waited for the Pinpoint ping.  Updated UDP reading to non-blocking.
     *
     * @return  A Boolean indicating the success of the operation
     * @throws IOException
     */
    public Boolean respondToUdpPing() throws IOException {
        ByteBuffer udpBuffer = ByteBuffer.allocate(SIZE_UDP_PING);
        byte[] clientPingResponse = new byte[SIZE_UDP_PING];

        SocketAddress client = datagramChannel.receive(udpBuffer);

        if (client != null)   {
            udpBuffer.flip();
            udpBuffer.get(clientPingResponse, 0, SIZE_UDP_PING);

            if (clientPingResponse[0] == (byte) 0x91)   {
                logger.debug(ILogger.TAG_GPS, "Client received UDP Ping.");

                clientPingResponse[0] = (byte) 0x81;

                ByteBuffer udpResponse = ByteBuffer.allocate(11);
                udpResponse.put(clientPingResponse);
                udpResponse.flip();
                datagramChannel.send(udpResponse, client);

                logger.debug(ILogger.TAG_GPS, "Client responded to UDP Ping.");

                return new Boolean(true);
            }
            else   {
                logger.error(ILogger.TAG_GPS, "Error receiving UDP Ping. First value is not 0x91.");
                return new Boolean(false);
            }
        }

        return new Boolean(true);

    }

    /**
     * Closes the associated nio resources
     *
     * @throws Exception
     */
    public void terminate()   {

        try   {
            if (socketChannel != null)  {
                socketChannel.socket().close();
                socketChannel.close();
            }
            if (datagramChannel != null)  {
                datagramChannel.socket().close();
                datagramChannel.close();
            }
        }
        catch(IOException ioe)   {
            logger.error(ILogger.TAG_GPS, "Error stopping GpsConsumer: ", ioe);
        }

        logger.info(ILogger.TAG_GPS, "GpsConsumer shutdown.");
    }

    /**
     * Provide mechanism to override serverIp so that we can configure production GPS Pinpoint, but set to localhost
     * for tests.
     *
     * @param serverIp
     */
    public void setServerIp(String serverIp)   {
        this.serverIp = serverIp;
    }
}
