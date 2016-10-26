package com.leidos.glidepath.dvi.simulated.testconsumers;

import com.leidos.glidepath.appcommon.DataElement;
import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.DataElementKey;
import com.leidos.glidepath.appcommon.SpatMessageDataElement;
import com.leidos.glidepath.asd.AsdMessageType;
import com.leidos.glidepath.asd.IAsdMessage;
import com.leidos.glidepath.asd.UdpForwardConsumerConfigured;
import com.leidos.glidepath.asd.spat.SpatMessage;

public class SpatUtilConsumer extends UdpForwardConsumerConfigured {

    public SpatUtilConsumer()   {
        super();
        setPort(7788);
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
        }

        return rtn;
    }


}
