package com.leidos.glidepath.asd;

import java.io.*;
import java.net.*;

import com.leidos.glidepath.appcommon.Constants;

public class SimulatedAsd {
	
	public static void main(String[] args) {
		
		String host;
		if (args.length > 0){
			host = args[0];
		}else {
			host = "localhost";
		}
		
		new SimulatedAsd().test(host);
	}
	
	public void test(String host) {
		
		//set up the network environment
		InetAddress addr = null;
		try {
			if (host == "localhost"){
				addr = InetAddress.getLocalHost();
			}else {
				addr = InetAddress.getByName(host);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		int port = 7789; //Integer.valueOf(config.getProperty("asd.mapport"));
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e1) {
			e1.printStackTrace();
			return;
		}
		System.out.println("Sending messages to " + host + ", port " + port);
		
		byte[] buf = null;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String input = "";
		
		//loop until exit command
		int cmd = 0;
		do {
			
			//display the menu of commands available
			System.out.println(" ");
			System.out.println("Select an action:");
			System.out.println("0) Exit");
			System.out.println("1) Send complete MAP message v1");
			System.out.println("2) Send complete MAP message v2");
			System.out.println("3) Send MAP message with extra MAP ID byte");
			System.out.println("4) Send incomplete MAP message");
			System.out.println("5) Send MAP with extraneous object");
			System.out.println("9) Send garbage");
			
			//wait for a command
			try {
				input = in.readLine();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			cmd = Integer.valueOf(input);
			
			
			//execute the user's command
			switch (cmd) {
			//the test driver is expecting the first four messages to be SPATs
			case 0:
				buf = null;
				break;
				
			//a real MAP message (v1)
			case 1:
				buf = load("CompleteMapMessage1");
				break;
				
			//a real MAP message (v2)
			case 2:
				buf = load("CompleteMapMessage2");
				break;
				
			//MAP message with a bogus ID flag in front of it
			case 3:
				buf = load("BogusMarkerPriorToCompleteMapMessage3");
				break;
				
			//incomplete MAP message
			case 4:
				buf = load("IncompleteMapMessage4");
				break;
				
			//valid MAP message with extraneous object
			case 5:
				buf = load("MapMessageExtraneousObject5");
				break;
				
			//garbage
			case 9:
			default:
				buf = unknownMsg;
				break;
			}
			
			
			//broadcast it on the specified UDP port
			try {
				if (buf != null) {
					DatagramPacket packet = new DatagramPacket(buf, buf.length, addr, port);
					socket.send(packet);
				}
			} catch (IOException e) {
				e.printStackTrace();
				socket.close();
				return;
			}
			
		} while(cmd != 0);
		
		System.out.println("Shutting down.");
		socket.close();
	}
	
	///////////////////////////////////////////////
	
	private void randomMsg() {
		
		//wait a random amount of time
		double r = Math.random();
		double delay = 3.8*r + 1.2;
		System.out.print("Wait " + delay + " sec...");
		try {
			Thread.sleep((int)(1000.0*delay));
		} catch (InterruptedException e) {
			//do nothing
		}
		
		/*****
		//randomly choose whether to generate a SPAT, MAP or unknown message type
		int type = (int)(3.0*Math.random());
		//..but guarantee we get at least one of each of the real types
		if ((MAX_MESSAGES - msgCount) <= 2) {
			if (numSpat == 0){
				type = 0;
			}else if (numMap == 0){
				type = 1;
			}
		}
		
		//generate the message content
		clearBuffer(buf);
		switch(type){
		case 0:
			buf = spatMsg;
			++numSpat;
			System.out.println("Sending fake SPAT");
			break;
		case 1:
			int sel = (int)(Math.random()*3);
			switch (sel) {
			case 0:
				buf = load("MapMessage1ExtraConnection");
				break;
			case 1:
				buf = load("MapMessageExtraneousObject");
				break;
			default:
				buf = load("CompleteMapMessage1");
			}
			++numMap;
			System.out.println("Sending real MAP #" + sel);
			break;
		default:
			buf = unknownMsg;
			System.out.println("Sending unknown message");
		}
		*****/
	}
	
	private byte[] load(String testName) {
		byte[] buf = null;
		String filename = "testdata/" + testName + ".dat";
		try {
			FileInputStream is = new FileInputStream(filename);
			buf = new byte[Constants.ASD_MAX_PACKET_SIZE];
			int num = is.read(buf);
			if (num <= 0) {
				is.close();
				throw new IOException("No bytes read from file.");
			}
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return buf;
	}

	private static final byte[] spatMsg =		{ (byte) 0xfe, (byte) 0x8d, (byte) 0xfc, (byte) 0xfb, (byte) 0xfa, (byte) 0xf9 };
	private static final byte[] unknownMsg =	{ (byte) 0x98, (byte) 0xf0, (byte) 0xf1, (byte) 0xf2 };
}
