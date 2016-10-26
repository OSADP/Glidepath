package com.leidos.glidepath.dvi.services;

import com.leidos.glidepath.appcommon.DataElementHolder;
import com.leidos.glidepath.appcommon.XgvStatusDataElement;
import com.leidos.glidepath.dvi.simulated.testutils.XgvUtils;
import com.leidos.glidepath.xgv.messages.ReportDiscreteDevicesMessage;
import com.leidos.glidepath.xgv.XgvStatus;
import org.junit.Test;

import static com.leidos.glidepath.appcommon.DataElementKey.XGV_STATUS;
import static com.leidos.glidepath.xgv.messages.ReportDiscreteDevicesMessage.XgvGearState.NEUTRAL;
import static org.junit.Assert.*;

public class SimulatedXgvTester {


    @Test
    public void doXgvStatus()   {

        DataElementHolder holder = new DataElementHolder();

        XgvStatus xgvStatus = new XgvStatus(false, false, false, NEUTRAL);
        XgvStatusDataElement element = new XgvStatusDataElement(xgvStatus);
        holder.put(XGV_STATUS, element);

        XgvStatus status = ((XgvStatusDataElement) holder.get(XGV_STATUS)).value();

        assertTrue(status.getGear() == NEUTRAL);

        holder.clear();

        xgvStatus = new XgvStatus(false, false, false, XgvUtils.randomEnum(ReportDiscreteDevicesMessage.XgvGearState.class));

        for (int i=0; i<10; i++) {
            Object gear = XgvUtils.randomEnum(ReportDiscreteDevicesMessage.XgvGearState.class);
        }

        return;

    }


}
