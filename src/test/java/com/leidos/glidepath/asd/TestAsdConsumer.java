package com.leidos.glidepath.asd;

import static org.junit.Assert.*;

import com.leidos.glidepath.asd.map.MapConsumer;
import com.leidos.glidepath.asd.map.MapMessage;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.*;
import com.leidos.glidepath.logger.*;
import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;

//This test driver should be run concurrently with the ASD simulator to pass it datagrams.
//Otherwise, its tests will probably fail.  Therefore, it is not named ...Tester so that
//it won't get run automatically by maven.

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class TestAsdConsumer {
	
    @Autowired
    ApplicationContext applicationContext;

	@Before
	public void setup() {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	log_.info("TEST", "===== NEW TEST BEGUN =====");

		LoggerManager.setOutputFile("logs/AsdConsumerTest.txt");
}
	
	@Test
	public void consolidatedTest() {
		//Need to do one big test here that calls all the others to ensure that they are properly sequenced.
		//Sequencing is important because the message server (SimulatedAsd) on the other machine is pumping
		//messages ordered to support these tests in this order.
		
		try {
			//set up the test environment
			System.out.println("### READY TO BEGIN TESTS - start the message server now, then wait for prompts.");
			System.out.println("### Be sure that the dvi.properties file has a long enough asd.operTimeout value set.");
			Thread.sleep(5000);
			
			//test the initializer
			countdown("Initializer - requires a valid MAP msg");
			testInitializer();
			LoggerManager.writeToDisk();
			System.out.println("testInitializer complete");

			//test a good MAP message
			countdown("Complete MAP, v1");
			testMap();
			LoggerManager.writeToDisk();
			System.out.println("testMap complete");

			//test a good MAP message - again
			countdown("Complete MAP, v2");
			testMap();
			LoggerManager.writeToDisk();
			System.out.println("testMap complete");

			//test a valid MAP message with an extra MAP ID byte in front of it
			countdown("MAP with bogus ID before it");
			testBogusMarkerPriorToCompleteMapMessage();
			LoggerManager.writeToDisk();
			System.out.println("testBogusMarkerPriorToCompleteMapMessage complete");
			
			//test an incomplete MAP message
			countdown("Incomplete MAP message");
			testIncompleteMapMessage();
			LoggerManager.writeToDisk();
			System.out.println("testIncompleteMapMessage complete");
			
			//test a MAP message with an extraneous object (invalid message)
			countdown("MAP with extraneous object");
			testMapWithExtraneousObject();
			LoggerManager.writeToDisk();
			System.out.println("testMapWithExtraneousObject complete");
			
			/***** the following are for SPAT tests
			testCall1();
			LoggerManager.writeToDisk();
			System.out.println("testCall1 complete");
			
			testCall3();
			LoggerManager.writeToDisk();
			System.out.println("testCall3 complete");
			*****/
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@After
	public void shutdown() {

		try {
			log_.info("TEST", "Entering shutdown");
			LoggerManager.writeToDisk();
			consumer_.terminate();
			
		}catch (Exception e) {
			//do nothing for now
		}
	}
	
	public void testInitializer() {
		
        //create the consumer object and initialize it
		consumer_ = new MapConsumer();
		assert(consumer_ != null);
		boolean res = consumer_.initialize();
		assertTrue(res);
		IConsumerInitializer init = consumer_.getInitializer();
		try {
			Boolean callRes = init.call();
			assertTrue((boolean)callRes);
		} catch (Exception e) {
			log_.error("TEST", "Initializer failed.");
			e.printStackTrace();
		}

	}
	
	public void testMap() {
		log_.info("TEST", "Entering testMap");
		
		try {
			//there may be a MAP message, but not guaranteed, so loop until we find one
			
			MapMessageDataElement map = null;
			int count = 0;
			do {
				DataElementHolder d = consumer_.call();

				if ((map = (MapMessageDataElement)d.get(DataElementKey.MAP_MESSAGE)) != null) {
					log_.infof("TEST", "Found a MAP msg on %dth try", count);
					MapMessage msg = map.value();
					double rlat = msg.getRefPoint().lat();
					assertTrue(rlat > 38.94  &&  rlat < 38.97);
					Lane lane0 = msg.getLane(0);
					assertTrue(lane0.isApproach());
					assertTrue(lane0.id() == 12);
					Location nodes0[] = lane0.getNodes();
					assertTrue(nodes0.length == 29);
					Lane lane4 = msg.getLane(4);
					assertTrue(lane4.width() == 400);
					Location nodes4[] = lane4.getNodes();
					assertTrue(nodes4.length == 8);
					log_.info("TEST", "MAP processing test complete.");
				}
				++count;
			} while (map == null  &&  count < 5);
			
			if (map == null) {
				log_.errorf("TEST", "Did not receive a valid MAP in %d tries", count);
			}
			
		}catch (Exception e){
			log_.errorf("TEST", e.toString());
		}
	}

    public void testBogusMarkerPriorToCompleteMapMessage() {
    	log_.debug("TEST", "Entering testBogusMarkerPriorToCompleteMapMessage");

		try {
			//there may be a MAP message, but not guaranteed, so loop until we find one
			
			MapMessageDataElement map = null;
			int count = 0;
			do {
				DataElementHolder d = consumer_.call();

				if ((map = (MapMessageDataElement)d.get(DataElementKey.MAP_MESSAGE)) != null) {
					log_.infof("TEST", "Found a MAP msg on %dth try", count);
					MapMessage msg = map.value();
			    	assertEquals(msg.intersectionId(), 1901); //the TFHRC test intersection
			    	assertEquals(msg.getRefPoint().lat(), 38.95, 0.02);
			    	assertEquals(msg.getRefPoint().lon(), -77.15, 0.02);
			    	assertTrue(msg.numLanes() >= 4);
			    	Lane lane0 = msg.getLane(0);
			    	assertNotNull(lane0);
			    	assertTrue(lane0.getNodes().length > 3);
					log_.info("TEST", "MAP processing test complete.");
				}
				++count;
			} while (map == null  &&  count < 5);
			
			if (map == null) {
				log_.errorf("TEST", "Did not receive a valid MAP in %d tries", count);
			}
			assert(map != null);
			
		}catch (Exception e){
			log_.error("TEST", e.toString());
		}
    }
    
    public void testIncompleteMapMessage() {
    	log_.debug("TEST", "Entering testIncompleteMapMessage");
    	
    	//only try it once since we expect no valid message to be received
    	MapMessageDataElement map = null;
    	try {
			DataElementHolder d = consumer_.call();
			map = (MapMessageDataElement)d.get(DataElementKey.MAP_MESSAGE);
		} catch (Exception e) {
			log_.error("TEST", e.toString());
		}
    	assert(map == null);
    }
    
    public void testMapWithExtraneousObject() {
    	log_.debug("TEST", "Entering testMapWithExtraneousObject");
    	
    	//only try it once since we expect no valid message to be received
    	MapMessageDataElement map = null;
    	try {
			DataElementHolder d = consumer_.call();
			map = (MapMessageDataElement)d.get(DataElementKey.MAP_MESSAGE);
		} catch (Exception e) {
			log_.error("TEST", e.toString());
		}
    	assert(map == null);
    }

	public void testCall1() {
		log_.info("TEST", "Entering testCall1");

		try {
			DataElementHolder d = consumer_.call();
			assertTrue(d.size() >= 4);
			assertTrue(((PhaseDataElement)d.get(DataElementKey.SIGNAL_PHASE)).value() == SignalPhase.GREEN);
			double dist1 = ((DoubleDataElement)d.get(DataElementKey.DIST_TO_STOP_BAR)).value();
			assertTrue(dist1 == 190.0);
			double time1 = ((DoubleDataElement)d.get(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE)).value();
			assertTrue(time1 == 35.0);
			
		}catch (Exception e){
			log_.errorf("TEST", e.toString());
		}
	}
	
	public void testCall3() {
		log_.info("TEST", "Entering testCall3");

		try {
			//first call
			DataElementHolder d = consumer_.call();
			
			//second call
			d = consumer_.call();
			double dist2 = ((DoubleDataElement)d.get(DataElementKey.DIST_TO_STOP_BAR)).value();
			assertTrue(dist2 > 0  &&  dist2 < 190.0);
			double time2 = ((DoubleDataElement)d.get(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE)).value();
			assertTrue(time2 > 0  &&  time2 < 35.0);

			//third call
			d = consumer_.call();
			assertTrue(d.size() >= 4);
			assertTrue(((PhaseDataElement)d.get(DataElementKey.SIGNAL_PHASE)).value() == SignalPhase.GREEN);
			double dist3 = ((DoubleDataElement)d.get(DataElementKey.DIST_TO_STOP_BAR)).value();
			assertTrue(dist3 > 0  &&  dist3 < dist2);
			double time3 = ((DoubleDataElement)d.get(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE)).value();
			assertTrue(time3 > 0  &&  time3 < time2);
		}catch (Exception e){
			log_.errorf("TEST", e.toString());
		}
	}

	//////////////////
	// private members
	//////////////////
	
	private void countdown(String name) {
		try {
			System.out.print("Ready to test " + name + ": 5 ");
			for (int i = 4;  i >= 0;  --i) {
				Thread.sleep(1000);
				System.out.print(i + " ");
			}
			System.out.println("Send data.");
		} catch (InterruptedException e) {
			//do nothing
		}
	}
	
	private MapConsumer consumer_;
	private static ILogger		log_ = LoggerManager.getLogger(TestAsdConsumer.class);
	
}
