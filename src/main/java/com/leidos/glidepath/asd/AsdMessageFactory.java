package com.leidos.glidepath.asd;

import com.leidos.glidepath.asd.map.MapMessage;
import com.leidos.glidepath.asd.spat.SpatMessage;

/**
 * Factory class to create a concrete IAsdMessage interface class
 *
 * User: ferenced
 * Date: 1/19/15
 * Time: 9:22 AM
 *
 */
public class AsdMessageFactory {

    public static IAsdMessage newInstance(AsdMessageType msgType)   {
        IAsdMessage message = null;

        switch (msgType)   {
            case MAP_MSG_ID:
                message = new MapMessage();
                break;

            case SPAT_MSG_ID:
                message = new SpatMessage();
                break;

            default:
                break;
        }

        return message;
    }
}
