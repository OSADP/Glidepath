package com.leidos.glidepath.appcommon.utils;

import com.leidos.glidepath.IConsumerInitializer;
import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.asd.spat.LaneSet;
import com.leidos.glidepath.asd.spat.Movement;
import com.leidos.glidepath.asd.spat.SpatMessage;
import com.leidos.glidepath.dvi.simulated.testconsumers.SpatUtilConsumer;

public class SpatUtil {

    public static void main(String args[])
            throws java.io.IOException
    {
        System.out.println("Args: " + args.length);

        SpatUtilConsumer spatConsumer = new SpatUtilConsumer();

        boolean bInitialized = spatConsumer.initialize();

        if (!bInitialized)   {
            System.out.println("Error initializing SpatUtilConsumer.");
            System.exit(1);
        }

        try   {
            IConsumerInitializer initializer = spatConsumer.getInitializer();
            bInitialized = initializer.call();
        }
        catch(Exception e)   {
            System.out.println("Exception caught: " + e.getMessage());
            System.exit(1);
        }

        if (!bInitialized)   {
            System.out.println("Initializer failed...exiting app.");
            System.exit(1);
        }

        try  {

            while (true)   {

                DataElementHolder holder = spatConsumer.call();

                DataElement dataElement = holder.get(DataElementKey.SPAT_MESSAGE);
                if (dataElement != null)   {
                    SpatMessageDataElement spatElement = (SpatMessageDataElement) dataElement;
                    SpatMessage spat = spatElement.value();


                    if (args.length == 0)   {
                        DataElementHolder raw = spat.getRawSpatForLane(12);

                        StringBuffer sb = new StringBuffer();
                        PhaseDataElement phaseElement = (PhaseDataElement) raw.get(DataElementKey.SIGNAL_PHASE);
                        double minTime = raw.getDoubleElement(DataElementKey.SIGNAL_TIME_TO_NEXT_PHASE);
                        double maxTime = raw.getDoubleElement(DataElementKey.SIGNAL_TIME_TO_THIRD_PHASE);
                        sb.append(phaseElement.value().toString());
                        sb.append(" : ");
                        sb.append(Double.toString(minTime) + " : " + Double.toString(maxTime));

                        System.out.println(sb.toString());
                    }
                    else   {
                        System.out.println(spat.getSpatMessageAsString());
                    }

                }

            }

        }
        catch(Exception e)   {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

}
