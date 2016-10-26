package com.leidos.glidepath.filter;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.logger.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import com.leidos.glidepath.filter.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class MatrixSolverTester {

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void setup() {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
        config_ = context.getAppConfig();
        LoggerManager.setRecordData(true);
    }
    
    @Test
    public void test1() {
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering test1");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
		MatrixSolver solver = new MatrixSolver();
		solver.setAElement(0, 0, 1.0);
		solver.setAElement(0, 1, 1.0);
		solver.setAElement(0, 2, 1.0);
		
		solver.setAElement(1, 0, 4.0);
		solver.setAElement(1, 1, 3.0);
		solver.setAElement(1, 2, 4.0);
		
		solver.setAElement(2, 0, 9.0);
		solver.setAElement(2, 1, 3.0);
		solver.setAElement(2, 2, 4.0);
		
		solver.setBElement(0, 3.0);
		solver.setBElement(1, 8.0);
		solver.setBElement(2, 7.0);
		
		double[] res = new double[3];
		try {
			res = solver.getResult();
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(-0.2, res[0], 0.001);
		assertEquals(4.0, res[1], 0.001);
		assertEquals(-0.8, res[2], 0.001);
    }
    
    @Test
    public void test2() { //same as test1 but with rows 1 & 3 swapped
		try {
			Thread.sleep(2);
			log_.debug("TEST", "===== Entering test2");
			Thread.sleep(2); //to force the log file to be sequenced more correctly
		} catch (InterruptedException e1) {
			// do nothing
		}
    	
		MatrixSolver solver = new MatrixSolver();
		solver.setAElement(0, 0, 9.0);
		solver.setAElement(0, 1, 3.0);
		solver.setAElement(0, 2, 4.0);
		
		solver.setAElement(1, 0, 4.0);
		solver.setAElement(1, 1, 3.0);
		solver.setAElement(1, 2, 4.0);
		
		solver.setAElement(2, 0, 1.0);
		solver.setAElement(2, 1, 1.0);
		solver.setAElement(2, 2, 1.0);
		
		solver.setBElement(0, 7.0);
		solver.setBElement(1, 8.0);
		solver.setBElement(2, 3.0);
		
		double[] res = new double[3];
		try {
			res = solver.getResult();
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(-0.2, res[0], 0.001);
		assertEquals(4.0, res[1], 0.001);
		assertEquals(-0.8, res[2], 0.001);
    }
    
    @After
    public void shutdown() {
		try {
			LoggerManager.setOutputFile("logs/MatrixSolver.txt");
			LoggerManager.writeToDisk();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    //////////////////
    // private members
    //////////////////
    
    private AppConfig			config_;
	private static Logger		log_ = (Logger)LoggerManager.getLogger(MatrixSolverTester.class);
}
