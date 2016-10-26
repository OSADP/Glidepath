package com.leidos.glidepath.ead;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.logger.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.asd.*;
import com.leidos.glidepath.asd.map.MapMessage;
import com.leidos.glidepath.dvi.AppConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class IntersectionTester {

    @Autowired
    ApplicationContext applicationContext;

    @Before
	public void setup() {

        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
		AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
		respectTimeouts_ = Boolean.valueOf(config.getProperty("performancechecks"));
        LoggerManager.setRecordData(true);

        try {
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    }
    
    @Test
    public void testLineSegment2D() {
    	log_.debug("TEST", "===== Entering testLineSegment2D =====");
    	
    	try {
    		//set up end points of line segments
	    	CartesianPoint2D end1 = new CartesianPoint2D(2.3, 1.0);
	    	CartesianPoint2D end2 = new CartesianPoint2D(4.3, 1.0);
	    	CartesianPoint2D end3 = new CartesianPoint2D(1.0, 1.0);
	    	CartesianPoint2D end4 = new CartesianPoint2D(1.0, 4.0);
	    	CartesianPoint2D end5 = new CartesianPoint2D(-1.0, 3.0);
	    	CartesianPoint2D end6 = new CartesianPoint2D(-0.7, -2.0);
	    	CartesianPoint2D pb = null;

	    	//set up test points for distances to line segments
	    	CartesianPoint2D p = new CartesianPoint2D(3.0, 2.9);
	    	CartesianPoint2D q = new CartesianPoint2D(1.0, 2.0);
	    	CartesianPoint2D r = new CartesianPoint2D(-1.0, -1.0);
	    	CartesianPoint2D s = new CartesianPoint2D(-53.0, 53.0);
	    	CartesianPoint2D t = new CartesianPoint2D(3.0, -1.0);

	    	//case 1: horiz segment & P above it
			LineSegment2D s1 = new LineSegment2D(end1, end2);
	    	assertEquals(s1.shortestDistanceToPoint(p), 1.9, 0.0001);
	    	assertEquals(s1.shortestDistanceToPointExtendedSegment(p), 1.9, 0.0001);
	    	pb = s1.translateTo(p);
	    	assertEquals(pb.x(), 3.0, 0.0001);
	    	assertEquals(pb.y(), 1.0, 0.0001);
	    	pb = s1.translateToExtendedSegment(p);
	    	assertEquals(pb.x(), 3.0, 0.0001);
	    	assertEquals(pb.y(), 1.0, 0.0001);
    	
	    	//case 2: vert segment & P to the right of it
			LineSegment2D s2 = new LineSegment2D(end3, end4);
	    	assertEquals(s2.shortestDistanceToPoint(p), 2.0, 0.0001);
	    	assertEquals(s2.shortestDistanceToPointExtendedSegment(p), 2.0, 0.0001);
	    	pb = s2.translateTo(p);
	    	assertEquals(pb.x(), 1.0, 0.0001);
	    	assertEquals(pb.y(), 2.9, 0.0001);
	    	pb = s2.translateToExtendedSegment(p);
	    	assertEquals(pb.x(), 1.0, 0.0001);
	    	assertEquals(pb.y(), 2.9, 0.0001);
	    	
	    	//case 3: vert segment with Q on top of it
	    	assertEquals(s2.shortestDistanceToPoint(q), 0.0, 0.0001);
	    	assertEquals(s2.shortestDistanceToPointExtendedSegment(q), 0.0, 0.0001);
	    	pb = s2.translateTo(q);
	    	assertEquals(pb.x(), 1.0, 0.0001);
	    	assertEquals(pb.y(), 2.0, 0.0001);
	    	pb = s2.translateToExtendedSegment(q);
	    	assertEquals(pb.x(), 1.0, 0.0001);
	    	assertEquals(pb.y(), 2.0, 0.0001);
	    	
	    	//case 4: angled segment with Q to the right
	    	LineSegment2D s4 = new LineSegment2D(end3, end5);
	    	assertEquals(s4.shortestDistanceToPoint(q), 0.707, 0.001);
	    	assertEquals(s4.shortestDistanceToPointExtendedSegment(q), 0.707, 0.001);
	    	pb = s4.translateTo(q);
	    	assertEquals(pb.x(), 0.5, 0.0001);
	    	assertEquals(pb.y(), 1.5, 0.0001);
	    	pb = s4.translateToExtendedSegment(q);
	    	assertEquals(pb.x(), 0.5, 0.0001);
	    	assertEquals(pb.y(), 1.5, 0.0001);
	    	
	    	//case 5: angled segment with point in 4th quadrant
	    	LineSegment2D s5 = new LineSegment2D(end1, end6);
	    	assertEquals(s5.shortestDistanceToPoint(r), 0.919, 0.002);
	    	assertEquals(s5.shortestDistanceToPointExtendedSegment(r), 0.919, 0.002);
	    	
	    	//case 6: same as 5 but segment end points reversed
	    	LineSegment2D s6 = new LineSegment2D(end6, end1);
	    	assertEquals(s6.shortestDistanceToPoint(r), 0.919, 0.002);
	    	assertEquals(s6.shortestDistanceToPointExtendedSegment(r), 0.919, 0.002);
	    	
	    	//case 7: point not between the segment end points
	    	assertEquals(s6.shortestDistanceToPoint(p), -1.0, 0.0001);
	    	assertEquals(s6.shortestDistanceToPointExtendedSegment(p), 0.849, 0.002);
	    	pb = s6.translateTo(p);
	    	assertNull(pb);
	    	pb = s6.translateToExtendedSegment(p);
	    	assertNotNull(pb);
	    	assertEquals(pb.x(), 3.6, 0.2);
	    	assertEquals(pb.y(), 2.3, 0.2);
	    	
	    	//case 8: point not between the segment end points
	    	assertEquals(s1.shortestDistanceToPoint(r), -1.0, 0.0001);
	    	assertEquals(s1.shortestDistanceToPointExtendedSegment(r), 2.0, 0.0001);
	    	
	    	//case 9: point way far away, but between the end points
	    	assertEquals(s5.shortestDistanceToPoint(s), 76.0, 0.5);
	    	assertEquals(s5.shortestDistanceToPointExtendedSegment(s), 76.0, 0.5);
	    	
	    	//case 10: point beyond segment endpoint, but on extended line
	    	assertEquals(s4.shortestDistanceToPoint(t), -1.0, 0.0001);
	    	assertEquals(s4.shortestDistanceToPointExtendedSegment(t), 0.0, 0.0001);
	    	pb = s4.translateTo(t);
	    	assertNull(pb);
	    	pb = s4.translateToExtendedSegment(t);
	    	assertNotNull(pb);
	    	assertEquals(pb.x(), 3.0, 0.0001);
	    	assertEquals(pb.y(), -1.0, 0.0001);
	    	
	    	//case 11: point beyond segment endpoints, with vertical segment
	    	assertEquals(s2.shortestDistanceToPoint(r), -1.0, 0.0001);
	    	assertEquals(s2.shortestDistanceToPointExtendedSegment(r), 2.0, 0.0001);

    	} catch (Exception e) {
			log_.debug("TEST", "Error setting up a line segment somewhere");
		}

    	//case 12; attempt to define a segment of zero length
    	try{
	    	CartesianPoint2D enda = new CartesianPoint2D(-0.7, -2.0);
	    	CartesianPoint2D endb = new CartesianPoint2D(-0.7, -2.0);
    		LineSegment2D s12 = new LineSegment2D(enda, endb);
    		assertNull(s12);
    	} catch(Exception e) {
    		//Exception expected, but we don't need to do anything to pass the test
    		log_.debug("TEST", "Exception successfully trapped in creating a bogus line segment.");
    	}
    }
    
    @Test
    public void testLaneGeometry() {
    	log_.debug("TEST", "===== Entering testLaneGeometry =====");
    	
    	//create the MAP message and pull a lane out of it
    	MapMessageDataElement elem = (MapMessageDataElement)loadMap("MapLane12Egress9");
    	MapMessage map = elem.value();
    	Lane lane13 = map.getLane(1);
    	
    	//construct the lane geometry
    	LaneGeometry lg;
		try {
			lg = new LaneGeometry(map.getRefPoint(), lane13);
	    	assertNotNull(lg);
	    	
	    	//define some test vehicle locations
	    	Location boonies	= new Location(39.0, -77.5); //way out in left field
	    	Location nearS5		= new Location(38.954640, -77.149516);
	    	Location outsideP6	= new Location(38.954618, -77.149540);
	    	Location insideP6	= new Location(38.954620, -77.149475);
	    	Location beyondP10	= new Location(38.954453, -77.149183);
	    	
	    	assertFalse(lg.inBoundingBox(boonies));
	    	assertTrue(lg.cte(boonies) > 5000);
	    	
	    	assertTrue(lg.inBoundingBox(nearS5));
	    	assertTrue(lg.cte(nearS5) < 100);
	    	assertTrue(lg.dtsb(nearS5) < 3280  &&  lg.dtsb(nearS5) > 2587);
	    	
	    	assertTrue(lg.inBoundingBox(outsideP6));
	    	assertTrue(lg.cte(outsideP6) < 250  &&  lg.cte(outsideP6) > 0);
	    	assertTrue(lg.dtsb(outsideP6) > 3260  &&  lg.dtsb(outsideP6) < 3300);
	    	
	    	assertTrue(lg.inBoundingBox(insideP6));
	    	assertTrue(lg.cte(insideP6) > 0  &&  lg.cte(insideP6) < 500);
	    	assertTrue(lg.dtsb(insideP6) > 3250  &&  lg.dtsb(insideP6) < 3800);
	    	
	    	assertTrue(lg.inBoundingBox(beyondP10));
	    	assertTrue(lg.dtsb(beyondP10) > 6874  &&  lg.dtsb(beyondP10) < 6950);
		} catch (Exception e) {
			assertTrue(false);
		}

		//this test covers actual observed vehicle positional data; need to use the real intersection MAP
    	//create a new MAP message and pull a lane out of it
    	MapMessageDataElement elemObs = (MapMessageDataElement)loadMap("CompleteMapMessage1");
    	MapMessage mapObs = elemObs.value();
    	Lane lane12 = mapObs.getLane(0);
    	
    	//construct the lane geometry
    	LaneGeometry lgObs;
		try {
			lgObs = new LaneGeometry(mapObs.getRefPoint(), lane12);
	    	assertNotNull(lgObs);
	    	
	    	Location inNode15Wedge = new Location(38.9551496, -77.1469345);
	    	int wedgeCte = lgObs.cte(inNode15Wedge);
	    	int wedgeDtsb = lgObs.dtsb(inNode15Wedge);
	    	assertEquals(wedgeCte, 535, 10);
	    	assertTrue(wedgeDtsb > 0);
	    	
	    	//test data points from actual run 20150320.085337 (where it crossed the stop bar)
	    	Location stepBefore = new Location(38.9549467060714, -77.1491212025284);
	    	Location stepAfter  = new Location(38.9549472928047, -77.1491268184036);
	    	Location between1	= new Location(38.95494699,      -77.14912401);
	    	int cte = lgObs.cte(stepBefore);
	    	int dtsb = lgObs.dtsb(stepBefore);
	    	assertEquals(cte, 244, 2);
	    	assertEquals(dtsb, 30, 3);
	    	
	    	cte = lgObs.cte(stepAfter);
	    	dtsb = lgObs.dtsb(stepAfter);
	    	assertEquals(cte, 244, 2);
	    	assertEquals(dtsb, -20, 2);
	    	
	    	cte = lgObs.cte(between1);
	    	dtsb = lgObs.dtsb(between1);
	    	assertEquals(cte, 244, 2);
	    	assertEquals(dtsb, 6, 2);
	    	
		} catch (Exception e) {
			assertTrue(false);
		}
    }
    
    @Test
    public void testStopBoxWidth() {
    	log_.debug("TEST", "===== Entering testStopBoxWidth =====");
    	
    	MapMessageDataElement elem = (MapMessageDataElement)loadMap("MapLane12Egress9");
    	MapMessage map = elem.value();

    	Intersection i = new Intersection(respectTimeouts_);
    	try {
			i.initialize(map);
		} catch (Exception e) {
			e.printStackTrace();
		}

    	double w = i.stopBoxWidth();
    	assertEquals(25.0, w, 15.0);
    }
    
    @Test
    public void testIntersectionBoonies() { //vehicle is nowher that the intersection recognizes
    	log_.debug("TEST", "===== Entering testIntersectionBoonies =====");
    	
    	MapMessageDataElement elem = (MapMessageDataElement)loadMap("MapLane12Egress9");
    	MapMessage map = elem.value();

    	Intersection i = new Intersection(respectTimeouts_);
    	try {
			i.initialize(map);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	i.computeGeometry(BOONIES1_LAT, BOONIES1_LON);
    	assertEquals(i.laneId(), -1);
    	assertTrue(i.dtsb() > 1000.0);
    	assertTrue(i.cte() > 1000);
    }
    
    @Test
    public void testIntersectionL13Begin() { 
    	log_.debug("TEST", "===== Entering testIntersectionL13Begin =====");
    	
    	MapMessageDataElement elem = (MapMessageDataElement)loadMap("MapLane12Egress9");
    	MapMessage map = elem.value();

    	Intersection i = new Intersection(respectTimeouts_);
    	try {
			i.initialize(map);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	i.computeGeometry(L13S9_LAT, L13S9_LON);
    	assertEquals(i.laneId(), 13);
    	assertTrue(i.dtsb() > 58.10  &&  i.dtsb() < 68.70);
    	assertTrue(i.cte() < 300);
    }
    
    @Test
    public void testIntersectionL13InsideCurve() { 
    	log_.debug("TEST", "===== Entering testIntersectionL13InsideCurve =====");
    	
    	MapMessageDataElement elem = (MapMessageDataElement)loadMap("MapLane12Egress9");
    	MapMessage map = elem.value();

    	Intersection i = new Intersection(respectTimeouts_);
    	try {
			i.initialize(map);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	i.computeGeometry(L13_INSIDE_CURVE_LAT, L13_INSIDE_CURVE_LON);
    	//assertEquals(i.laneId(), -1); //this assertion holds if CTE threshold is < 800 cm, but we are using 1000 for demos
    	assertTrue(i.dtsb() > 1000.0);
    	assertTrue(i.cte() > 800);
    }
    
    //this test will only work if Intersection.LARGE_MOVEMENT is altered - most recently passed (manually) on 1/31/15
    @Ignore
    public void testIntersectionTraverseBox() { 
    	log_.debug("TEST", "===== Entering testIntersectionTraverseBox =====");
    	
    	MapMessageDataElement elem = (MapMessageDataElement)loadMap("MapLane12Egress9");
    	MapMessage map = elem.value();

    	Intersection i = new Intersection(respectTimeouts_);
    	try {
			i.initialize(map);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	//cruising down lane 13 to set up the previous time step state
    	i.computeGeometry(L13P1_LAT, L13P1_LON);
    	assertEquals(i.laneId(), 13);
    	assertTrue(i.cte() < 10);
    	
    	//nearing stop bar on lane 13
    	i.computeGeometry(L13S0_LAT, L13S0_LON);
    	assertEquals(i.laneId(), 13);
    	assertTrue(i.dtsb() > 0.5  &&  i.dtsb() < 1.0);
    	assertTrue(i.cte() < 300);
    	
    	//just before crossing the stop bar on lane 13
    	i.computeGeometry(L13P0_LAT, L13P0_LON);
    	assertEquals(i.laneId(), 13);
    	assertTrue(i.dtsb() > 0.0  &&  i.dtsb() < 0.5);
    	assertTrue(i.cte() < 20);
    	
    	//crossed the stop bar, early in the box
    	i.computeGeometry(ENTERING_STOP_BOX_LAT, ENTERING_STOP_BOX_LON);
    	assertEquals(i.laneId(), 13);
    	assertTrue(i.dtsb() < 0.0);
    	double dtsbEarlyBox = i.dtsb();
    	
    	//on the far side of the stop box, almost out of range of lane 13
    	i.computeGeometry(FAR_SIDE_STOP_BOX_LAT, FAR_SIDE_STOP_BOX_LON);
    	assertEquals(i.laneId(), 13);
    	assertTrue(i.dtsb() < dtsbEarlyBox);
    	double dtsbFarBox = i.dtsb();
    	
    	//picking up lane 44 for egress
    	i.computeGeometry(L44P0_LAT, L44P0_LON);
    	assertEquals(i.laneId(), 44);
    	assertTrue(i.dtsb() < dtsbFarBox);
    	assertTrue(i.cte() < 50);
    	double dtsbFound44 = i.dtsb();

    	//continuing down lane 44
    	i.computeGeometry(L44S0_LAT, L44S0_LON);
    	assertEquals(i.laneId(), 44);
    	assertTrue(i.dtsb() < dtsbFound44);
    	assertTrue(i.cte() < 300);
    }
    
	@After
	public void shutdown() {
        try {
			LoggerManager.setOutputFile("logs/IntersectionTest.txt");
			LoggerManager.writeToDisk();
		}catch (Exception e) {
			//do nothing for now
		}
	}


    //////////////////
    // member elements
    //////////////////
    
	private DataElement loadMap(String testName) {
		byte[] buf = null;
		MapMessage msg = null;
		String filename = "testdata/" + testName + ".dat";
		
		try {
			FileInputStream is = new FileInputStream(filename);
			buf = new byte[1400];
			int num = is.read(buf);
			if (num <= 0) {
				is.close();
				throw new IOException("No bytes read from file.");
			}
			is.close();
			
	    	msg = new MapMessage();
	    	msg.parse(buf); //not testing for success - only feed it valid data!
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		MapMessageDataElement elem = new MapMessageDataElement(msg);

    	return elem;
	}
	
	private boolean						respectTimeouts_;
	
	private static final double			BOONIES1_LAT					= 38.96;
	private static final double			BOONIES1_LON					= -77.16;
	private static final double			L13S9_LAT						= 38.954465;
	private static final double			L13S9_LON						= -77.149250;
	private static final double			L13_INSIDE_CURVE_LAT			= 38.954557;
	private static final double			L13_INSIDE_CURVE_LON			= -77.149280;
	private static final double			L13P1_LAT						= 38.954877; //exactly on P1
	private static final double			L13P1_LON						= -77.149383;
	private static final double			L13S0_LAT						= 38.954881;
	private static final double			L13S0_LON						= -77.149380; //around 10 cm CTE
	private static final double			L13P0_LAT						= 38.954884; //very slightly south-west of P0 (still before the stop bar)
	private static final double			L13P0_LON						= -77.149376;
	private static final double			ENTERING_STOP_BOX_LAT			= 38.954896; //about 4-5m beyond stop bar
	private static final double			ENTERING_STOP_BOX_LON			= -77.149345;
	private static final double			FAR_SIDE_STOP_BOX_LAT			= 38.954894; //getting close to the lane 44 stop bar (our egress route)
	private static final double			FAR_SIDE_STOP_BOX_LON			= -77.149200;
	private static final double			L44P0_LAT						= 38.954925;
	private static final double			L44P0_LON						= -77.1491286; //very slightly west of P0
	private static final double			L44S0_LAT						= 38.954922; //half way between nodes 0 & 1
	private static final double			L44S0_LON						= -77.149098;

    private static ILogger				log_ = LoggerManager.getLogger(IntersectionTester.class);
}
