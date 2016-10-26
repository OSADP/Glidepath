package com.leidos.glidepath.gps;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.gps.simulated.SimulatedGpsProducer;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimulatedGpsProducerTester {


    @Test
    public void csvGpsData()   {
        SimulatedGpsProducer producer = new SimulatedGpsProducer("testdata/gpsGlobalPositions.csv");

        producer.load();
        assertTrue(producer.size() == 440);

        DataElementHolder holder = producer.getGpsData();

        assertTrue(holder.size() > 0);

        //38.957198, -77.145701
        assertTrue(holder.getLatitude() == 38.957198);
        assertTrue(holder.getLongitude() == -77.145701);
    }
}
