package com.leidos.glidepath.can;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.appcommon.utils.UnpackUtils;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.ILogger;
import com.leidos.glidepath.logger.LoggerManager;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Utility to test an OBD2 ELM327 USB device
 *
 * This utility was just used for due diligence trying to understand CAN bus.  It is still unclear to me
 * exactly how the CAN bus will appear to our on board Ubuntu computer and whether we need to translate
 * between CAN Frames and this utility using OBD2.
 *
 * More research needed on SocketCAN and can-utils.
 *
 * Purchased via Amazon for $11.
 *
 * Linux drivers are already available.  Windows drivers provided on CD, but I did not test on Windows.
 *
 * When you connect the device, it appears as /dev/ttyUSB0.
 *
 * I used the jSSC serial port library to write and read.  It accepts modem commands and OBD2 commands.
 * http://en.wikipedia.org/wiki/OBD-II_PIDs
 *
 * You can simply send a command (RPM ==> 01 0C\r.....and the device responds...ascii char representation of hex bytes)
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class Obd2Tester {
    private ILogger logger = LoggerManager.getLogger(Obd2Tester.class);

    private final static String PORT = "/dev/ttyUSB0";
    private final static int TIMEOUT = 500;
    private final static int SLEEP = 20;

    private final static int RPM_RESPONSE_SIZE = 34;
    private final static int MAF_RESPONSE_SIZE = 21;

    private final static String PID_RPM = "01 0C";
    private final static String PID_SPEED = "01 0D";


    // PID (hex)	Data bytes returned	Description	Min value	Max value	Units	Formula[a]
    // 10	    2	MAF air flow rate	0	655.35	grams/sec	((A*256)+B) / 100
    private final static String PID_MAF = "01 10";

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void before()   {
        LoggerManager.setOutputFile("logs/speedcontrol.log");
        logger.info("",  "");
        logger.info("====", "=============================");
        logger.info("CAN", "Obd2Tester started.");
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
    }


    @Ignore
    public void testRpm() throws IOException   {
        writeReadData(PID_RPM, RPM_RESPONSE_SIZE);
    }

    @Ignore
    public void testMaf() throws IOException   {
        DateTime startTime = new DateTime();
        writeReadData(PID_MAF, MAF_RESPONSE_SIZE);
        logDuration(startTime);
    }

    @Test
    public void dummyTest() {

    }

    public boolean writeReadData(String pid, int responseSize) throws IOException {
        boolean bResult = false;

        String data = pid + "\r";

        SerialPort serialPort = new SerialPort(PORT);
        try {
            bResult = serialPort.openPort();//Open serial port
            logger.debugf("CAN", "Serial port " + PORT + " opened: %b", bResult);

            if (bResult)   {
                serialPort.setParams(SerialPort.BAUDRATE_38400,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

                bResult = serialPort.writeBytes(data.getBytes());
                logger.debugf("CAN", "Wrote bytes to serial port: %b", bResult);

                String response = readData(serialPort);

                if (response != null && !response.isEmpty())   {
                    String formatted = response.replaceAll("\r", "\n");
                    System.out.println("Response: \n" + formatted);
                    logger.info("SERIAL", "Response: \n" + formatted);

                    String byteString = response.substring(12, 17);
                    System.out.println("Value Bytes: " + byteString);
                    logger.info("SERIAL", "Value Bytes: " + byteString);

                    String byteStringNoSpace = byteString.replaceAll(" ", "");
                    double value = calculateMafFromResponse(byteStringNoSpace);

                    System.out.println("Calculated Value: " + value);
                    logger.info("SERIAL", "CalculatedValue: " + value);
                }

                serialPort.closePort();//Close serial port
                logger.debug("CAN", "Closed serial port.");

                try   {
                    LoggerManager.writeToDisk();
                }
                catch(IOException ioe) {};
            }
        }
        catch (SerialPortException ex) {
            logger.error("CAN", "Error writing to serial port: " + ex.getMessage());
            throw new IOException(ex);

        }

        return bResult;
    }

    /**
     * Read OBD2 response from device
     * Reads until > character received up to TIMEOUT value
     *
     * @param serialPort
     * @return String    String Hex representation of bytes
     */
    private String readData(SerialPort serialPort)   {

        DateTime startTime = new DateTime();
        StringBuffer sb = new StringBuffer();

        try   {
            while (true)  {

                int bytesAvailable = serialPort.getInputBufferBytesCount();

                if (bytesAvailable > 0)    {
                    String data = serialPort.readString(bytesAvailable);
                    sb.append(data);
                    if (data.contains(">"))   {
                        break;
                    }
                }

                try  {
                    Thread.sleep(SLEEP);
                }
                catch(Exception e) {}

                DateTime currentTime = new DateTime();
                Duration dur = new Duration(startTime, currentTime);
                long cycleLasted = dur.getMillis();

                if (cycleLasted >= TIMEOUT)   {
                    break;
                }
            }

        }
        catch(SerialPortException spe)   {

        }

        return sb.toString();
    }


    // 0C	2	Engine RPM	0	16,383.75	rpm	((A*256)+B)/4
    public double calculateRpmFromResponse(String bytesAsString)   {
        byte[] theByte = toByteArray(bytesAsString);

        int a = UnpackUtils.getInstance().unpackU8(theByte[0]);
        int b = UnpackUtils.getInstance().unpackU8(theByte[1]);

        double rpm = ( ( a * 256 ) + b) / 4;

        return rpm;
    }


    // 10	    2	MAF air flow rate	0	655.35	grams/sec	((A*256)+B) / 100
    public double calculateMafFromResponse(String bytesAsString)   {
        byte[] theByte = toByteArray(bytesAsString);

        int a = UnpackUtils.getInstance().unpackU8(theByte[0]);
        int b = UnpackUtils.getInstance().unpackU8(theByte[1]);

        double rpm = ( ( a * 256 ) + b) / 100;

        return rpm;
    }


    public String toHexString(byte[] array) {
        return DatatypeConverter.printHexBinary(array);
    }

    public byte[] toByteArray(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }


    public long logDuration(DateTime startTime)   {
        DateTime endTime = new DateTime();
        Duration duration = new Duration(startTime, endTime);
        long cycle = duration.getMillis();

        System.out.println("Serial Read took: " + cycle);
        logger.info("SERIAL", "Serial Read took: " + cycle);

        return cycle;
    }

    /**
     * Open/restore permissions to connected USB device so non-root users can read/write
     *
     * @param flag     true, then set to 666, false, set to 660
     * @return boolean indicating success of permission change
     */
    public boolean changeUsbPermissions(boolean flag)   {
        boolean result = true;

        Path usb = Paths.get(PORT);
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-rw-rw-");

        // if flag is false, set back to 660
        if (!flag)   {
            perms = PosixFilePermissions.fromString("rw-rw----");
        }

        try   {
            Files.setPosixFilePermissions(usb, perms);
        }
        catch(Exception e)   {
            System.out.println("Error changing USB port permissions: " + e.getMessage());
            logger.info("SERIAL", "Error changing USB port permissions: " + e.getMessage());
            result = false;
        }

        return result;
    }
}
