package com.leidos.glidepath.asd.spat;

import com.leidos.glidepath.appcommon.*;
import com.leidos.glidepath.appcommon.utils.GlidepathApplicationContext;
import com.leidos.glidepath.asd.AsdMessageType;
import com.leidos.glidepath.asd.IAsdMessage;
import com.leidos.glidepath.asd.UdpForwardConsumer;

/**
 * SPAT Consumer
 *
 * User: ferenced
 * Date: 1/18/15
 * Time: 11:18 PM
 *
 */
public class SpatConsumer extends UdpForwardConsumer {

    public SpatConsumer()   {
        super();
        setPort(Integer.valueOf(GlidepathApplicationContext.getInstance().getAppConfig().getProperty("asd.spatport")));
        setMsgType(AsdMessageType.SPAT_MSG_ID);
    }

    @Override
    public DataElementHolder processMessage(IAsdMessage message) {

        //package the results into a data holder and return it
        DataElementHolder rtn = new DataElementHolder();

        if (message instanceof SpatMessage)   {
            SpatMessage spatMessage = (SpatMessage) message;
            DataElement dataElement = new SpatMessageDataElement(spatMessage);
            rtn.put(DataElementKey.SPAT_MESSAGE, dataElement);
            spatMessage.dumpSpatMessage();
        }

        return rtn;
    }


}
