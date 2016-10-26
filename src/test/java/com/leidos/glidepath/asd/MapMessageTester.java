package com.leidos.glidepath.asd;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.logger.*;
import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.asd.map.MapMessage;
import com.leidos.glidepath.dvi.AppConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class MapMessageTester {

    @Autowired
    ApplicationContext applicationContext;
    
    @Before
    public void startup() {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);

		try {
			Thread.sleep(3); //force better sequencing of logs
		} catch (InterruptedException e) {
			//do nothing
		}
    }
    
    @Test
    public void testLocation() {
    	log_.debug("TEST", "Entering testLocation");
		
		final double REF_LAT = 35.555;
		final double REF_LON = -88.1247;
		Location ref = new Location(REF_LAT, REF_LON);
    	
    	Location loc1 = new Location(ref, 142, -3600);
    	
    	Location loc2 = new Location(ref, 543, 12);
    	
    	int dist = loc1.distanceFrom(loc2);
    	assertEquals(dist, 3634);
    	assertEquals(loc1.lon(), REF_LON + 142.0/Constants.RADIUS_OF_EARTH_METERS*1.8/Constants.PI/Math.cos(REF_LAT*RAD_PER_DEG), 0.00001); //dlon = dist/Re*180/pi/100
    	
    	loc2.setOffset(ref, -4405, -999);
    	dist = loc1.distanceFrom(loc2);
    	assertEquals(dist, 5239, 1);
    	dist = loc2.distanceFrom(loc1);
    	assertEquals(dist, 5239, 1);
    	assertEquals(loc2.lat(), REF_LAT -999.0/Constants.RADIUS_OF_EARTH_METERS*1.8/Constants.PI, 0.00001);
    }
    
    @Test
    public void testLane() {
    	log_.debug("TEST", "Entering testLane");

		final double REF_LAT = 35.555;
		final double REF_LON = -88.1247;
		Location ref = new Location(REF_LAT, REF_LON);
    	
		Lane lane1 = new Lane();
		lane1.setApproach(true);
		lane1.setId(2233);
		lane1.setWidth(4200);
		lane1.setAttributes(0x04a3);
		lane1.addNodeCm(ref, 0, 600);		//point coords should be (0, 600)
		lane1.addNodeCm(ref, 234, 500);	//point coords should be (234, 1100)
		lane1.addNodeCm(ref, 520, -108);	//point coords should be (754, 992)
		assertEquals(lane1.id(), 2233);
		assertTrue(lane1.isApproach());
		assertEquals(lane1.attributes(), 1187); //1187 dec = 0x04a3
		Location[] nodes1 = lane1.getNodes();
		assertEquals(nodes1.length, 3);
		assertEquals(nodes1[2].lat(), REF_LAT + 992.0/Constants.RADIUS_OF_EARTH_METERS*1.8/Constants.PI, 0.00001);
		assertEquals(nodes1[2].lon(), REF_LON + 754.0/Constants.RADIUS_OF_EARTH_METERS*1.8/Constants.PI/Math.cos(REF_LAT*RAD_PER_DEG), 0.00003);
		
		Lane lane2 = new Lane();
		lane2.setApproach(false);
		lane2.setId(66778);
		lane2.setWidth(4988);
		lane2.setAttributes(0x6666);
		lane2.addNodeDm(ref, -508, 12);	//note that this one is in decimeters
		lane2.addNodeDm(ref, -47, 377);
		lane2.addNodeDm(ref, 9, 102);
		lane2.addNodeDm(ref, 189, -5);
		assertEquals(lane2.id(), 66778);
		assertEquals(lane2.attributes(), 26214); //26214 dec = 0x6666
		assertFalse(lane2.isApproach());
		Location[] nodes2 = lane2.getNodes();
		assertEquals(nodes2.length, 4);
		assertEquals(nodes2[1].lon(), REF_LON -5550.0/Constants.RADIUS_OF_EARTH_METERS*1.8/Constants.PI/Math.cos(REF_LAT*RAD_PER_DEG), 0.00003);
		assertEquals(nodes2[2].lat(), REF_LAT + 4910.0/Constants.RADIUS_OF_EARTH_METERS*1.8/Constants.PI, 0.00001);
		assertEquals(nodes2[3].lon(), REF_LON -3570.0/Constants.RADIUS_OF_EARTH_METERS*1.8/Constants.PI/Math.cos(REF_LAT*RAD_PER_DEG), 0.00003);
		assertEquals(nodes2[3].lat(), REF_LAT + 4860.0/Constants.RADIUS_OF_EARTH_METERS*1.8/Constants.PI, 0.00001);
    }
    
    @Test
    public void testNoMapMessage() {
    	log_.debug("TEST", "Entering testNoMapMessage");

    	byte[] buf = load("NoMapMessage"); //MAP msgID doesn't exist in this short file
    	MapMessage msg = new MapMessage();
    	boolean res = msg.parse(buf);
    	assertFalse(res);
    }

    @Test
    public void testIncompleteMapMessage1() {
    	log_.debug("TEST", "Entering testIncompleteMapMessage");

    	byte[] buf = load("IncompleteMapMessage4"); //valid MAP header exists, and first part of msg only
    	MapMessage msg = new MapMessage();
    	boolean res = msg.parse(buf);
    	assertFalse(res);
    }

    @Test
    public void testCompleteMapMessage1() {
    	log_.debug("TEST", "Entering testCompleteMapMessage1");

    	byte[] buf = load("CompleteMapMessage1"); //valid MAP message w/CRC, no extraneous bytes
    	MapMessage msg = new MapMessage();
    	boolean res = msg.parse(buf);
    	assertTrue(res);
    	assertEquals(msg.intersectionId(), 1901); //the TFHRC test intersection
    	double refLat = msg.getRefPoint().lat();
    	double refLon = msg.getRefPoint().lon();
    	assertEquals(refLat, 38.95, 0.05);
    	assertEquals(refLon, -77.18, 0.05);
    	assertTrue(msg.numLanes() >= 4);
    	Lane lane0 = msg.getLane(0);
    	assertNotNull(lane0);
    	assertEquals(lane0.getNodes().length, 29);
    	
    	Location[] nodes0 = lane0.getNodes();
    	double lat0 = nodes0[0].lat();
    	double lon0 = nodes0[0].lon();
    	assertTrue(lat0 < refLat);
    	assertTrue(lon0 > refLon);
    	double lat1 = nodes0[1].lat();
    	double lon1 = nodes0[1].lon();
    	assertTrue(lat1 < lat0);
    	assertTrue(lon1 > lon0);
    	
    	Location[] nodes1 = msg.getLane(1).getNodes();
    	lat0 = nodes1[0].lat();
    	lon0 = nodes1[0].lon();
    	assertTrue(lat0 < refLat);
    	assertTrue(lon0 < refLon);
    }
    
    @Test
    public void testBogusMarkerPriorToCompleteMapMessage() {
    	log_.debug("TEST", "Entering testBogusMarkerPriorToCompleteMapMessage");

    	byte[] buf = load("BogusMarkerPriorToCompleteMapMessage3"); //valid MAP message w/CRC, but extra bytes in front, including a valid MAP header
    	MapMessage msg = new MapMessage();
    	boolean res = msg.parse(buf);
    	assertFalse(res);
    }

    @Test
    public void testInvalidMapMessageLength() {
    	log_.debug("TEST", "Entering testInvalidMapMessageLength");

    	byte[] buf = load("InvalidMapMessageLength6"); //valid MAP message, but header length is corrupted (too large)
    	MapMessage msg = new MapMessage();
    	boolean res = msg.parse(buf);
    	assertFalse(res);
    }

    @Test
    public void testMapMessageExtraneousObject() {
    	log_.debug("TEST", "Entering testMapMessageExtraneousObject");

    	byte[] buf = load("MapMessageExtraneousObject5"); //valid MAP message, but includes object of unknown ID (0x63); should fail
    	MapMessage msg = new MapMessage();
    	boolean res = msg.parse(buf);
    	assertFalse(res);
    }

    @Test
    public void testMapMessageCorruptObjectLength() {
    	log_.debug("TEST", "Entering testMapMessageCorruptObjectLength");

    	byte[] buf = load("MapMessageCorruptObjectLength7"); //valid MAP message, but length indicator for reference point is only 6
    	MapMessage msg = new MapMessage();
    	boolean res = msg.parse(buf);
    	assertFalse(res);
    }

    @Test
    public void testMapMessage1ExtraConnection() {
    	log_.debug("TEST", "Entering testMapMessage1ExtraConnection");

    	byte[] buf = load("MapMessage1ExtraConnection8"); //valid MAP message w/CRC, extra connection object that should be ignored
    	MapMessage msg = new MapMessage();
    	boolean res = msg.parse(buf);
    	assertTrue(res);
    	assertEquals(msg.intersectionId(), 1901); //the TFHRC test intersection
    	assertEquals(msg.getRefPoint().lat(), 38.95, 0.05);
    	assertEquals(msg.getRefPoint().lon(), -77.18, 0.05);
    	assertTrue(msg.numLanes() >= 4);
    	Lane lane0 = msg.getLane(0);
    	assertNotNull(lane0);
    	assertTrue(lane0.getNodes().length > 3);
    }
    
    @Test
    public void testEgressLane() {
    	log_.debug("TEST", "Entering testEgressLane");

    	byte[] buf = load("MapLane12Egress9"); //one lane is an egress
    	MapMessage msg = new MapMessage();
    	boolean res = msg.parse(buf);
    	assertTrue(res);
    	Lane lane0 = msg.getLane(0);
    	assertEquals(lane0.id(), 44);
    	assertFalse(lane0.isApproach());
    }

	@After
	public void shutdown() {
		try {
			LoggerManager.setOutputFile("logs/MapMessageTest.txt");
			LoggerManager.writeToDisk();
		}catch (Exception e) {
			//do nothing for now
		}
	}
		
	//////////////////
	// private members
	//////////////////
	
	private byte[] load(String testName) {
		byte[] buf = null;
		String filename = "testdata/" + testName + ".dat";
		try {
			FileInputStream is = new FileInputStream(filename);
			buf = new byte[MAX_SIZE];
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

	private static Logger				log_ = (Logger)LoggerManager.getLogger(MapMessageTester.class);
	private final int					MAX_SIZE = 1600;
	private final double				RAD_PER_DEG = Constants.PI/180.0;
}
