package com.leidos.glidepath.xgv;

import com.leidos.glidepath.appcommon.utils.BitStreamPacker;
import com.leidos.glidepath.appcommon.utils.BitStreamUnpacker;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests relationship between BitStreamPacker and BitStreamUnpacker to ensure that they are inverses of each other
 */
public class BitUtilsIntegrationTester {
    private int numRandomTests = 100;

    private void testPackAndUnpack(int data) {
        BitStreamPacker p = new BitStreamPacker();
        p.writeInt(data);

        byte[] encoded = p.getBytes();

        BitStreamUnpacker u = new BitStreamUnpacker(encoded);
        int decoded = u.readInt();

        assertTrue("Message not properly encoded"
                        + "Original = " + data
                        + "; Encoded = " + Integer.toHexString(data)
                        + "; Decoded = " + Integer.toHexString(decoded)
                        + "; Final Value = " + decoded,
                        data == decoded);
    }

    @Test
    public void testNullPackAndUnpack() {
        testPackAndUnpack(0);
    }

    @Test
    public void testSmallPackAndUnpack() {
        testPackAndUnpack(12);
    }

    @Test
    public void testLargePackAndUnpack() {
        testPackAndUnpack(125941519);
    }

    @Test
    public void testRandomPackAndUnpack() {
        for (int i = 0; i < numRandomTests; i++) {
            BitStreamPacker p = new BitStreamPacker();

            int original = (int) (Math.random() * Integer.MAX_VALUE);
            p.writeInt(original);

            byte[] encoded = p.getBytes();

            BitStreamUnpacker u = new BitStreamUnpacker(encoded);
            int decoded = u.readInt();

            assertTrue("Message not properly encoded"
                            + "Original = " + original
                            + "; Encoded = " + Integer.toHexString(original)
                            + "; Decoded = " + Integer.toHexString(decoded)
                            + "; Final Value = " + decoded,
                            original == decoded);
        }
    }

    @Test
    public void testRandomLittleEndianIntPackAndUnpack() {
        for (int i = 0; i < numRandomTests; i++) {
            BitStreamPacker p = new BitStreamPacker();
            p.setLittleEndian();

            int original = (int) (Math.random() * Integer.MAX_VALUE);
            p.writeInt(original);

            byte[] encoded = p.getBytes();

            BitStreamUnpacker u = new BitStreamUnpacker(encoded);
            u.setLittleEndian();
            int decoded = u.readInt();

            assertTrue("Message not properly encoded"
                            + "Original = " + original
                            + "; Encoded = " + Integer.toHexString(original)
                            + "; Decoded = " + Integer.toHexString(decoded)
                            + "; Final Value = " + decoded,
                            original == decoded);
        }
    }

    @Test
    public void testRandomLittleEndianShortPackAndUnpack() {
        for (int i = 0; i < numRandomTests; i++) {
            BitStreamPacker p = new BitStreamPacker();
            p.setLittleEndian();

            short original = (short) (Math.random() * Short.MAX_VALUE);
            p.writeShort(original);

            byte[] encoded = p.getBytes();

            BitStreamUnpacker u = new BitStreamUnpacker(encoded);
            u.setLittleEndian();
            int decoded = u.readShort();

            assertTrue("Message not properly encoded"
                        + "Original = " + original
                        + "; Encoded = " + Integer.toHexString(original)
                        + "; Decoded = " + Integer.toHexString(decoded)
                        + "; Final Value = " + decoded,
                        original == decoded);
        }
    }
}
