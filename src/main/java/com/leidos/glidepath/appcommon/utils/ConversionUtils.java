package com.leidos.glidepath.appcommon.utils;

import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.dvi.domain.GlidepathState;
import java.util.Random;
import static com.leidos.glidepath.appcommon.Constants.*;


/**
 * Glidepath conversion utilities
 */
public class ConversionUtils {

    private static final Random r = new Random();
    private static final int low = 10;
    private static final int high = 30;

    private ConversionUtils() {
    }

    private static class ConversionUtilsHolder  {
        private static final ConversionUtils _instance = new ConversionUtils();
    }

    public static ConversionUtils getInstance()
    {
        return ConversionUtilsHolder._instance;
    }

    /**
     * Convert meters to second to miles per hour
     * @param mps
     * @return
     */
    public double mpsToMph(double mps)   {
        return mps * MPS_TO_MPH;
    }

    /**
     * Convert miles per hour to meters per second
     * @param mph
     * @return
     */
    public double mphToMps(double mph)   {
        return mph / MPS_TO_MPH;
    }

    /**
     * Convert meters to feet
     *
     * @param meters
     * @return
     */
    public double metersToFeet(double meters)   {
        return meters * METERS_TO_FEET;
    }

    /**
     * Convert feet to meters
     *
     * @param feet
     * @return
     */
    public double feetToMeters(double feet)   {
        return feet / METERS_TO_FEET;
    }


    public String formatDouble2(double value)   {
        return String.format( "%.2f", value);
    }

    public String formatDouble5(double value)   {
        return String.format( "%.5f", value);
    }

    /**
     * Add a random speed as the SPEED element to existing holder, used for simulation purposes
     * @param holder
     * @return
     */
    public DataElementHolder getRandomSpeed(DataElementHolder holder)   {
        int speed = r.nextInt(high - low) + low;

        DataElement speedElement = new DoubleDataElement(ConversionUtils.getInstance().mphToMps(speed));
        holder.put(DataElementKey.SPEED, speedElement);

        return holder;
    }
}