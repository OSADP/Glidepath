package com.leidos.glidepath.xgv.messages;

import com.leidos.glidepath.appcommon.utils.BitStreamUnpacker;
import com.leidos.glidepath.xgv.XgvIntegerConverter;

/**
 * Parses and makes available the message payload of a Report Velocity State Message (0x4404)
 */
public class ReportVelocityMessage extends JausMessage {
    private double velocity;

    public ReportVelocityMessage(JausMessage copy) {
        super(copy);
        parsePayload();
    }

    public ReportVelocityMessage(byte[] packet) {
        super(packet);
        parsePayload();
    }

    private void parsePayload() {
        BitStreamUnpacker u = new BitStreamUnpacker(getPayload(), true);
        int presenceVector = u.readShort();
        int xVelocity = u.readInt();

        // Convert Scaled Signed int to double.
        velocity = XgvIntegerConverter.signedScaledToReal(xVelocity, 65.534, -65.534, 32);
    }

    public double getVelocity() {
        return velocity;
    }
}
