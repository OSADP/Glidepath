package com.leidos.glidepath.appcommon;

import org.junit.Test;
import static org.junit.Assert.*;

import com.leidos.glidepath.appcommon.DataElementKey;

public class DataElementHolderTester {

    @Test
    public void putSomeValues()   {

        DataElementHolder holder = new DataElementHolder();

        holder.put(DataElementKey.SPEED, new DoubleDataElement(19.91));
        holder.put(DataElementKey.LATITUDE, new DoubleDataElement(169.1234567));

        assertTrue(holder.size() == 2);

        DoubleDataElement dataElement = (DoubleDataElement) holder.get(DataElementKey.SPEED);
        double velocity = dataElement.value();
        assertTrue( velocity == 19.91);

        double speed = holder.getSpeed();
        assertTrue( speed == 19.91);

    }

    @Test
    public void verifyNoLatLong()   {
        DataElementHolder holder = new DataElementHolder();

        double lat = holder.getLatitude();
        assertTrue(lat == 0);

        double longitude = holder.getLongitude();
        assertTrue(longitude == 0);

    }
}
