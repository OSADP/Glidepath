package com.leidos.glidepath.dvi.domain;

import org.junit.Test;
import static org.junit.Assert.*;

import static com.leidos.glidepath.dvi.domain.GlidepathState.*;

public class GlidepathStateModelTester {

    @Test
    public void positiveLifeCycle()   {
        GlidepathStateModel stateModel = GlidepathStateModel.getInstance();
        stateModel.reset();

        assertTrue(stateModel.getState() == STARTUP);

        // change to setup state
        boolean result = stateModel.changeState(SETUP, null);
        assertTrue(result);

        result = stateModel.changeState(STANDBY, null);
        assertTrue(result);

        result = stateModel.changeState(ENGAGED, null);
        assertTrue(result);

        result = stateModel.changeState(DISENGAGED, null);
        assertTrue(result);

        result = stateModel.changeState(STANDBY, null);
        assertTrue(result);

    }


    @Test
    public void startupTransitions()   {
        GlidepathStateModel stateModel = GlidepathStateModel.getInstance();
        stateModel.reset();

        assertTrue(stateModel.getState() == STARTUP);

        // can't go to the following states
        boolean result = stateModel.changeState(STANDBY, null);
        assertFalse(result);
        assertTrue(stateModel.getState() == STARTUP);

        result = stateModel.changeState(ENGAGED, null);
        assertFalse(result);
        assertTrue(stateModel.getState() == STARTUP);

        result = stateModel.changeState(DISENGAGED, null);
        assertFalse(result);
        assertTrue(stateModel.getState() == STARTUP);

    }

}
