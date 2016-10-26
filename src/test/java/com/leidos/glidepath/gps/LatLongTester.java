package com.leidos.glidepath.gps;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;


public class LatLongTester {

    private NioUtils nioUtils = new NioUtils(null);


    @Test
    public void encodeDecode()   {

        double lat = 179.1234567890;
        byte[] array = new byte[4];

        nioUtils.encodeLatLong(lat, array, 0);
        double decodedLat = nioUtils.decodeLatLong(array, 0);

        assertTrue((float) lat == (float) decodedLat);


        lat = 145.1234;
        nioUtils.encodeLatLong(lat, array, 0);
        decodedLat = nioUtils.decodeLatLong(array, 0);
        assertTrue((float) lat == (float) decodedLat);

        lat = -145.1234321;
        nioUtils.encodeLatLong(lat, array, 0);
        decodedLat = nioUtils.decodeLatLong(array, 0);
        assertTrue((float) lat == (float) decodedLat);

    }

}
