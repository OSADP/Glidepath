package com.leidos.glidepath.ead;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class EadFactoryTester {

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
    }

	@Test
	public void testDynamics() {
		IEad ead = EadFactory.newInstance("com.leidos.glidepath.ead.EadDynamics");
		
		assertTrue(ead instanceof EadDynamics);
		assertFalse(ead instanceof EadSimple);
		assertFalse(ead instanceof EadUcrJava);
	}
	
	@Test
	public void testUcrJava() {
		IEad ead = EadFactory.newInstance("com.leidos.glidepath.ead.EadUcrJava");
		
		assertTrue(ead instanceof EadUcrJava);
		assertFalse(ead instanceof EadDynamics);
		assertFalse(ead instanceof EadSimple);
	}
	
	@Test
	public void testSimple() {
		IEad ead = EadFactory.newInstance("com.leidos.glidepath.ead.EadSimple");
		
		assertTrue(ead instanceof EadSimple);
		assertFalse(ead instanceof EadUcrJava);
		assertFalse(ead instanceof EadDynamics);
	}
	
	@Test
	public void testBogus() {
		IEad ead = EadFactory.newInstance("com.leidos.glidepath.ead.Bogus");
		
		assertTrue(ead instanceof EadSimple);
		assertFalse(ead instanceof EadUcrJava);
		assertFalse(ead instanceof EadDynamics);
	}
}
