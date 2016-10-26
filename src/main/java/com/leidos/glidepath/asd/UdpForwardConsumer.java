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
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.asd.map.MapConsumer;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.*;
import com.leidos.glidepath.logger.*;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public abstract class UdpForwardConsumer implements IConsumerTask {

    public UdpForwardConsumer() {

        AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();

        //get timeout budgets from the app config
        boolean perfCheck = Boolean.valueOf(config.getProperty("performancechecks"));
        if (perfCheck) {
        	initTimeout_ = Integer.valueOf(config.getProperty("asd.initialTimeout"));
        	operTimeout_ = Integer.valueOf(config.getProperty("asd.operTimeout"));
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

        //get the most recent message from the device
        AsdDataPacket data = getLatestPacket(socket_, (int)operTimeout_, msgType_);

        //if a valid message was received then
        IAsdMessage msg = null;
        boolean msgPresent = false;
        if (data != null) {
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
					    msgPresent = true;
					    log_.debugf("ASD", "call: parsed the message of type %s", msgType_.toString());
					}
				} catch (Exception e) {
					log_.errorf("ASD", "Exception trapped from msg.parse(): %s", e.toString());
				}
            }
        }

        // delegate to derived class
        DataElementHolder rtn = new DataElementHolder();
        if (msgPresent) {
        	rtn.putAll(processMessage(msg));
        }

        Duration duration = new Duration(new DateTime(startTime), new DateTime());
        if (this instanceof MapConsumer)  {
            rtn.put(DataElementKey.CYCLE_MAP, new IntDataElement((int) duration.getMillis()));
        }
        else   {
            rtn.put(DataElementKey.CYCLE_SPAT, new IntDataElement((int) duration.getMillis()));
        }
        log_.infof("ASD", "call: completed for msgType = %s in %d ms with messages processed.", msgType_.toString(), (System.currentTimeMillis() - startTime));

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
    
    /**
     * > 0 data packets queued up on socket : most recent data packet
     * no data packets queued up but one received within timeout period : newly received data packet
     * no data packets queued up and none received in timeout period : null
     */
    protected AsdDataPacket getLatestPacket(DatagramSocket sock, int timeout, AsdMessageType type) throws Exception {
    	AsdDataPacket latest = null;
    	
    	//start the timer
    	long startTime = System.currentTimeMillis();
    	
    	//Note: there may be multiple packets queued up on the UDP port. If this is the case then it will take only
    	// a couple ms to pull off each one. Once they are all cleaned out, then we can afford to wait for the
    	// remainder of the timeout period to see if a fresh one comes in.  If not, we will keep the latest of
    	// the pre-existing ones.
    	int numRead = 0;
    	do {
    		//troll for a data packet, and if one is received then
        	AsdDataPacket data = new AsdDataPacket(sock, timeout, type);
    		if (data.read()) {
    			//store it for future reference as the latest to date
    			latest = data;
    			++numRead;
    		}
    	//while timeout hasn't expired
    	} while(System.currentTimeMillis() - startTime < timeout);
    	log_.debugf("ASD", "getLatestPacket pulled %d %s packets off the socket", numRead, type.toString());
    	
    	//return the latest message we've seen
    	return latest;
    }

    private boolean					msgNeverParsed_;	//have we never parsed a MAP message during the life of this object?
    private int						prevVersion_;	//content version of the previous MAP message we parsed
    private int						initTimeout_;
    private int						operTimeout_;
    private int						port_;		        //UDP port to listen for messages
    private DatagramSocket			socket_;
    private static Logger			log_ = (Logger)LoggerManager.getLogger(UdpForwardConsumer.class);
    private AsdInitializer			initializer_;
    private AsdMessageType          msgType_;          // MAP or SPAT
}
