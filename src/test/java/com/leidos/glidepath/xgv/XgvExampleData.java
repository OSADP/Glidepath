package com.leidos.glidepath.xgv;


public class XgvExampleData {

    public static byte[] QUERY_VELOCITY_STATE_0x2404 = {
            0x01, 0x00, 0x00, 0x00, 0x12,                // JUDP Header
            0x06, 0x02,                                  // priority, version ++
            0x04, 0x24,                                  // command code
            0x01, 0x2A, 0x01, 0x02,                      // dest jaus id instance, component, node, subsystem
            0x05, 0x05, (byte) 0xEE, (byte) 0xEE,        // source jaus id instance, component, node, subsystem
            0x00, 0x02,                                  // payload size ++
            0x00, 0x01,                                  // sequence ++
            (byte) 0xFF, (byte) 0xFF };                  // payload

    public static byte[] REPORT_VELOCITY_STATE_0x4404 = {
            0x01, 0x00, 0x00, 0x00, 0x1A,                // JUDP Header
            0x06, 0x02,                                  // priority, version ++
            0x04, 0x44,                                  // command code
            0x05, 0x05, (byte) 0xEE, (byte) 0xEE,        // dest jaus id instance, component, node, subsystem
            0x01, 0x2A, 0x01, 0x02,                      // source jaus id instance, component, node, subsystem
            0x0A, 0x00,                                  // payload size ++
            0x00, 0x01,                                  // sequence ++
            0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x45, 0x62, 0x35, (byte) 0x80        // payload
    };

    public static byte[] QUERY_DISCRETE_DEVICES_0x2406 = {
            0x01, 0x00, 0x00, 0x00, 0x11,                 // JUDP Header
            0x06, 0x02,                                   // priority, version ++
            0x06, 0x24,                                   // command code
            0x01, 0x21, 0x01, 0x02,                       // dest jaus id instance, component, node, subsystem
            0x05, 0x05, (byte) 0xEE, (byte) 0xEE,         // source jaus id instance, component, node, subsystem
            0x00, 0x01,                                   // payload size ++
            0x00, 0x01,                                   // sequence ++
            (byte) 0xFF                                   // payload
    };

    public static byte[] REPORT_DISCRETE_DEVICES_0x4406 = {
            0x01, 0x00, 0x00, 0x00, 0x14,
            0x06, 0x02,
            0x06, 0x44,
            0x05, 0x05, (byte) 0xEE, (byte) 0xEE,
            0x01, 0x21, 0x01, 0x02,
            0x04, 0x00,
            0x00, 0x01,
            0x07, 0x00, 0x00, 0x00
    };
}
