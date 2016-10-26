package com.leidos.glidepath.ead;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.dvi.AppConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class TrajectoryFactoryTester {

    private static final String SIMULATED = "com.leidos.glidepath.ead.SimulatedTrajectory";
    private static final String REAL = "com.leidos.glidepath.ead.Trajectory";

    @Autowired
    ApplicationContext applicationContext;

    @Before
    public void before()   {
        // set our singleton with the application context
        GlidepathApplicationContext context = GlidepathApplicationContext.getInstance();
        context.setApplicationContext(applicationContext);
    }

    @Test
    public void testSimulated()   {
        ITrajectory traj = TrajectoryFactory.newInstance(SIMULATED);

        assertTrue(traj instanceof SimulatedTrajectory);
        assertFalse(traj instanceof Trajectory);
    }

    @Test
    public void testReal()   {
        ITrajectory traj = TrajectoryFactory.newInstance(REAL);

        assertTrue(traj instanceof Trajectory);
        assertFalse(traj instanceof SimulatedTrajectory);
    }

    @Test
    public void testBadClass()   {
        ITrajectory traj = TrajectoryFactory.newInstance("com.leidos.glidepath.ead.BadClass");

        // bad class should return simulated wrapper
        assertTrue(traj instanceof SimulatedTrajectory);
        assertFalse(traj instanceof Trajectory);
    }
}
