package com.leidos.glidepath.asd;

import static org.junit.Assert.*;

import java.net.DatagramSocket;
import java.net.SocketException;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.logger.*;

//This test driver should be run concurrently with an ASD simulator to pass it datagrams.
//Otherwise, its tests will probably fail.  Therefore, it is not named ...Tester so that
//it won't get run automatically by maven.

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class TestAsdInitializer {
	
    @Autowired
    ApplicationContext applicationContext;

	@Before
	public void setup() {
    	log_.info("TEST", "===== NEW TEST BEGUN =====");
		
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
        AppConfig config = GlidepathApplicationContext.getInstance().getAppConfig();
        
        //set up to listen on the MAP port
        int port = Integer.valueOf(config.getProperty("asd.mapport"));
        int timeout = Integer.valueOf(config.getProperty("asd.initialTimeout"));
        DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}

        asd_ = new AsdInitializer(socket, timeout, AsdMessageType.MAP_MSG_ID);
	}
	
	@Test
	public void testCall0() {
		try {
			Boolean res = asd_.call(); //closes the socket when complete
			assertTrue((boolean)res);
		}catch (Exception e) {
			log_.errorf("TEST", "Trapped in testCall0: " + e.toString());
		}
	}

	@After
	public void shutdown() {
		log_.info("TEST", "Entering shutdown");
		try {
			LoggerManager.setOutputFile("logs/AsdInitializerTest.txt");
			LoggerManager.writeToDisk();
		}catch (Exception e) {
			log_.errorf("TEST", "Trapped in shutdown: " + e.toString());
		}
	}

	// private members
	
	private static ILogger	log_ = LoggerManager.getLogger(TestAsdInitializer.class);;
	private AsdInitializer	asd_;
}
