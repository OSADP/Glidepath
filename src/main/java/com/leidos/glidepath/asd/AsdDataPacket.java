package com.leidos.glidepath.asd;

import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.logger.*;

import java.io.IOException;
import java.net.*;

/**
 * This class represents an unknown candidate message pulled off the Arada ASD hardware, and does some basic validation
 * to gain confidence that it has found an actual DSRC message that will be of value.
 * 
 * @author starkj
 *
 */
public class AsdDataPacket {

	/**
	 * Sets up the raw message reader.
	 * 
	 * @param socket is an already-open UDP socket
	 * @param timeout is the allowable time for a successful read, in ms
     * @param msgType indicates eith MAP or SPAT message should be looked for
	 */
	public AsdDataPacket(DatagramSocket socket, int timeout, AsdMessageType msgType) {
		socket_ = socket;
		timeout_ = timeout;
		buffer_ = new byte[Constants.ASD_MAX_PACKET_SIZE];
        msgType_ = msgType;
	}
	
	/**
	 * Listens for a message from the ASD device for up to timeout ms.  If all of the following conditions pass then it is
	 * considered a successful message read and the content between start byte and end byte are preserved for future access, 
	 * otherwise it is considered a failure:
	 *   -if a message is received within timeout,
	 *   -the first LEADING_BYTES bytes contains either a MAP_MSG_ID or SPAT_MSG_ID (designated the start byte),
	 *   -at least 2 of the next 4 bytes, including the start byte (4 byte msg header) are non-zero,
	 *   -byte at (start byte + msg length - 1) is 0xff (designated as the end byte),
	 *   -if the CRC functionality is enabled, two bytes after end byte exist and message content passes the CRC check
	 *   -there are no parsing errors on the message content (creation of the MapMessage object)
	 * 
	 * @return true on successful read, false on failure
	 */
	public boolean read() {
		byte[] buf = new byte[Constants.ASD_MAX_PACKET_SIZE];
		DatagramPacket p = new DatagramPacket(buf, Constants.ASD_MAX_PACKET_SIZE);
		
		//read a packet from the UDP port
		long startTime = System.currentTimeMillis();
		try {
			socket_.setSoTimeout(timeout_);
			socket_.receive(p);
		} catch (SocketException e) {
			log_.infof("ASD", "read: socket exception after %d ms: %s", System.currentTimeMillis() - startTime, e.toString());
			return false;
		} catch (IOException e) {
			log_.infof("ASD", "read: IO exception after %d ms: %s", System.currentTimeMillis() - startTime, e.toString());
			return false;
		}
		buf = p.getData();
		log_.debugf("ASD", "read: buffer read from device, length = %d, took %d ms", buf.length, System.currentTimeMillis() - startTime);
		
		//if the buffer contains at least 6 bytes (allows for a 
		// 4 byte header, ending byte and at least one byte of message payload)
		boolean success = false;
		if (buf.length >= 6) {
			int start = 0; 
			
			//for each byte in the early part of the buffer
			for ( ; start < Math.min(buf.length, LEADING_BYTES);  ++start) {
		
				//if it is the MAP or SPAT message identifier then
				if ((buf[start] & 0x000000ff) == msgType_.getType()) {
		
					//if it does look like we have a whole, valid message here then indicate success and break out of the loop
					if (validateMessage(buf, start)) {
						success = true;
						break;
					}
				}
			}
		
			//if we believe we found a valid message then
			if (success) {
				//store the message part of the buffer for later
				int i = 0;
				int j = start;
				while (i < payloadLength_ + 4) { //account for the 4 header bytes
					buffer_[i++] = buf[j++];
				}
				log_.debugf("ASD", "device read was successful. Message header = %s", toString(4));
			}
		}
		
		return success;
	}
	
	/**
	 * Returns a buffer holding the message content (including msg header, but excluding any CRC)
	 */
	public byte[] getBuffer() {
		
		return buffer_;
	}
	
	/**
	 * Returns the published version of the message content
	 */
	public int contentVersion() {
		
		return contentVersion_;
	}

	/**
	 * Returns the number of bytes in the message payload (including the terminating 0xff byte, but excluding the 4 header bytes)
	 */
	public int payloadLength() {
		
		return payloadLength_;
	}
	
	/**
	 * Returns a string representation of the first n bytes of the buffer, in hex
	 * @param n: the number of bytes to convert
	 */
	public String toString(int n) {
		
		String out = new String();
		
		for (int i = 0;  i < Math.min(n, buffer_.length);  ++i){
			out += String.format("%02x ", buffer_[i]);
		}
		
		return out;
	}
	
	//////////////////
	// private members
	//////////////////

	/**
	 * buf contains 4 initial bytes, a payload, and an end byte (and optionally a valid CRC word) that indicate a valid message : true
	 * buf is too short, or the indicated bytes don't match expectations of a valid message : false
	 * 
	 * @param buf: buffer to be validated
	 * @param byte0: first byte in the buffer to start validating (ignore anything before this byte)
	 */
	private boolean validateMessage(byte[] buf, int byte0) {
		
		boolean valid = false;
		
		//if at least 2 of the first 4 bytes have a non-zero value then
		int nonZero = 0;
		for (int i = byte0;  i < byte0 + 4;  ++i) {
			if (buf[i] != 0) {
				++nonZero;
			}
		}
		if (nonZero >= 2) {
			
			//store the content version
			contentVersion_ = buf[byte0 + 1] & 0x000000ff;
		
			//determine the message length from bytes 2 & 3
			payloadLength_ = ((buf[byte0+2] << 8) & 0x0000ff00) | (buf[byte0+3] & 0x000000ff);
		
			//if the buffer has enough content to hold the indicated message length then
			if (buf.length >= byte0 + payloadLength_ + 4) {
		
				//if the indicated last byte of the message is a proper ending byte then
				if ((buf[byte0 + payloadLength_ + 3] & 0x000000ff) == 0xff) {
		
						//indicate that we have a valid message
						valid = true;
				}
			}
		}
		
		return valid;
	}
	

	private byte[]						buffer_; //the buffer holding this message's bytes
	private int							timeout_; //timeout to wait for a packet, ms
	private int							contentVersion_; //the broadcast version of the message content
	private int							payloadLength_;  //num bytes in message payload
	private DatagramSocket				socket_; //the UDP socket to listen to
	private static final int			LEADING_BYTES = 500; //the first bytes of the buffer to be searched for the beginning of a message
	private static Logger				log_ = (Logger)LoggerManager.getLogger(AsdDataPacket.class);
    private AsdMessageType              msgType_;  // ASD Message Type (MAP or SPAT)


}
