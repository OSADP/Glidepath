package com.leidos.glidepath.can;

/**
 * A Java copy of the linxu/can.h can_frame struct. Necessary for dealing with data passing through the JNI wrapper
 * around the SocketCAN interface.
 */
public class CanFrame {
    CanId id;
    int dataLengthCode;
    byte[] data;

    public static enum CanId {
        MASS_AIRFLOW_REQUEST (0x7DF),
        MASS_AIRFLOW_RESPONSE (0x7E9);

        private int id;
        CanId(int id) { this.id = id; }
        public int id() { return id; }
    }

    public CanFrame(CanId id, int dataLengthCode, byte[] data) {
        this.id = id;
        this.dataLengthCode = dataLengthCode;
        this.data = data;
    }

    public CanFrame(byte[] raw) {
        // Parse raw binary packet into CAN frame
    }

    public CanId getId() {
        return id;
    }

    public int getDataLengthCode() {
        return dataLengthCode;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] toBytes() {
        return null;
    }
}

