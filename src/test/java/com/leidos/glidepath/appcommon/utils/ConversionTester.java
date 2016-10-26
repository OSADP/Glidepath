package com.leidos.glidepath.appcommon.utils;


import org.junit.Test;
import static org.junit.Assert.*;

public class ConversionTester {

    @Test
    public void metersFeetTest()   {

        double feet = 3.2808;

        double meters = ConversionUtils.getInstance().feetToMeters(feet);

        assertTrue(meters == (double) 1.0 );

        feet = 1;
        meters = ConversionUtils.getInstance().feetToMeters(feet);

        assertTrue(meters == 1.0 / 3.2808) ;


        meters = 1;
        feet = ConversionUtils.getInstance().metersToFeet(meters);
        assertTrue(feet == 3.2808);

        meters = 100;
        feet = ConversionUtils.getInstance().metersToFeet(meters);
        String display = ConversionUtils.getInstance().formatDouble2(feet);

        assertTrue(feet >= 328.08);
        assertTrue(feet <= 328.081);
    }


}
