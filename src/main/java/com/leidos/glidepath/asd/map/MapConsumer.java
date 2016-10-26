package com.leidos.glidepath.asd.map;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.DataElementKey;
import com.leidos.glidepath.appcommon.MapMessageDataElement;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.asd.AsdMessageType;
import com.leidos.glidepath.asd.IAsdMessage;
import com.leidos.glidepath.asd.UdpForwardConsumer;

/**
 * MAP Consumer
 *
 * User: ferenced
 * Date: 1/19/15
 * Time: 5:57 PM
 *
 */
public class MapConsumer extends UdpForwardConsumer {

    public MapConsumer()   {
        super();
        setPort(Integer.valueOf(GlidepathApplicationContext.getInstance().getAppConfig().getProperty("asd.mapport")));
        setMsgType(AsdMessageType.MAP_MSG_ID);
    }

    @Override
    public DataElementHolder processMessage(IAsdMessage message) {
        DataElementHolder result = new DataElementHolder();

        if (message instanceof MapMessage && message != null)   {
            MapMessage mapMessage = (MapMessage) message;
            MapMessageDataElement elem = new MapMessageDataElement(mapMessage);
            result.put(DataElementKey.MAP_MESSAGE, elem);
        }

        return result;
    }
}
