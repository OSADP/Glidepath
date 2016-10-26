/**
 * UDP Forward Cnosumer
 *
 * Abstract previous AsdConsumer to provide framework for both MAP and SPAT messages
 *
 * User: ferenced
 * Date: 1/16/15
 * Time: 2:48 PM
 *
 */
package com.leidos.glidepath.asd;

import java.net.DatagramSocket;
import java.net.SocketException;

import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.*;
import com.leidos.glidepath.dvi.simulated.testconsumers.SpatUtilConsumer;
import com.leidos.glidepath.logger.*;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * clone of UdpForwardConsumer that just hard codes configuration for SpatUtil app
 *
 */
public abstract class UdpForwardConsumerConfigured implements IConsumerTask {

    public UdpForwardConsumerConfigured() {

        //get timeout budgets from the app config
        boolean perfCheck = true;
        if (perfCheck) {
            initTimeout_ = 10000;
            operTimeout_ = 40;
        }else {
            initTimeout_ = 8*3600000; //N hours, to allow for a detailed debugging session
            operTimeout_ = 8*3600000;
        }
        assert(initTimeout_ > 0);
        assert(operTimeout_ > 0);

        msgNeverParsed_ = true;
        prevVersion_ = 0;

    }

    /**
     * Subclass specific processing of message type
     *
     * @param IAsdMessage     A complete message parsed from a raw ASD packet
     * @return DataElementHolder
     */
    public abstract DataElementHolder processMessage(IAsdMessage message);


    @Override
    /**
     * Establishes a basic connection and handshake relationship with the ASD device
     *
     * Handshake established within timeout budget : true
     * Timeout budget expires without success : false
     */
    public boolean initialize() {

        try {
            //establish the UDP socket
            socket_ = new DatagramSocket(port_);

            //set up the initializer
            initializer_ = new AsdInitializer(socket_, initTimeout_, msgType_);
        } catch (SocketException e) {
            initializer_ = null;
            terminate();
            log_.errorf("ASD", "Unable to create a UDP socket for the ASD on port %d", port_);
        }

        return initializer_ != null;
    }

    @Override
    /**
     * Always : shut down the device connection
     */
    public void terminate() {

        if (socket_ != null) {
            socket_.close();
        }
    }

    @Override
    public IConsumerInitializer getInitializer() {

        return initializer_;
    }

    @Override
    /**
     * Collects the most recently available MAP & SPAT messages from the ASD device.
     *
     * MAP or SPAT available : returns available message(s)
     * No message retrieved within timeout budget : empty data element holder
     */
    public DataElementHolder call() throws Exception {

        //get current system time - start of local processing logic
        long startTime = System.currentTimeMillis();

        //create a data packet object to receive any content the device may be sending
        AsdDataPacket data = new AsdDataPacket(socket_, (int)operTimeout_, msgType_);

        //read a message from the device
        boolean looksLikeMessage = false;
        boolean timedOut = false;
        IAsdMessage msg = null;
        do {
            looksLikeMessage = data.read();
            timedOut = (System.currentTimeMillis() - startTime) >= operTimeout_;
        }while (!looksLikeMessage  &&  !timedOut);

        //reset current system time - start of local processing logic
        startTime = System.currentTimeMillis();

        //if a valid message was received then
        if (looksLikeMessage) {
            //if its version is different from the previously parsed message then
            if (msgNeverParsed_ ||  data.contentVersion() != prevVersion_) {
                log_.debug("ASD", "call: looks like we have a msg of interest. Ready to parse it.");

                //create a new message object and parse it
                msg = AsdMessageFactory.newInstance(msgType_);

                try {
                    byte[] buf = data.getBuffer();
                    if (msg.parse(buf)) {
                        //indicate that we have now parsed one
                        msgNeverParsed_ = false;
                        prevVersion_ = data.contentVersion();
                    }
                } catch (Exception e) {
                    log_.errorf("ASD", "Exception trapped from msg.parse(): %s", e.toString());
                }
            }
        }

        // delegate to derived class
        DataElementHolder rtn = processMessage(msg);

        log_.infof("ASD", "call: completed in %d ms with messages processed.", (System.currentTimeMillis() - startTime));

        Duration duration = new Duration(new DateTime(startTime), new DateTime());
        if (this instanceof SpatUtilConsumer)  {
            rtn.put(DataElementKey.CYCLE_SPAT, new IntDataElement((int) duration.getMillis()));
        }
        else   {
            rtn.put(DataElementKey.CYCLE_MAP, new IntDataElement((int) duration.getMillis()));
        }

        return rtn;
    }

    public void setPort(int port)   {
        this.port_ = port;
    }

    public void setMsgType(AsdMessageType msgType)   {
        this.msgType_ = msgType;
    }

    ////////////////////
    // member attributes
    ////////////////////

    private boolean					msgNeverParsed_;	//have we never parsed a MAP message during the life of this object?
    private int						prevVersion_;	    //content version of the previous MAP message we parsed
    private int						initTimeout_;
    private int						operTimeout_;
    private int						port_;		        //UDP port to listen for messages
    private DatagramSocket			socket_;
    private static Logger			log_ = (Logger)LoggerManager.getLogger(UdpForwardConsumer.class);
    private AsdInitializer			initializer_;
    private AsdMessageType          msgType_;          // MAP or SPAT
}
