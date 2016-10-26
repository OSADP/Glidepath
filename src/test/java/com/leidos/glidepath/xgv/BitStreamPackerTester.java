package com.leidos.glidepath.xgv;

import com.leidos.glidepath.appcommon.utils.BitStreamPacker;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test suite for BitStreamPacker.java
 * Tests to ensure that the data being passed into the BitStreamPacker is properly formatted and saved in a byte array
 */
public class BitStreamPackerTester {
    @Ignore
    public void testBitWriting() {
        BitStreamPacker packer = new BitStreamPacker();

        packer.writeBit(0);
        System.out.println(packer.getBytes()[0]);
        assertTrue(packer.getBytes()[0] == 0);

        packer.writeBit(1);
        System.out.println(packer.getBytes()[0]);
        assertTrue(packer.getBytes()[0] == 64);


        packer.writeBit(1);
        System.out.println(packer.getBytes()[0]);
        assertTrue(packer.getBytes()[0] == 96);

        packer.writeBit(0);
        System.out.println(packer.getBytes()[0]);
        assertTrue(packer.getBytes()[0] == 96);

        packer.writeBit(1);
        System.out.println(packer.getBytes()[0]);
        assertTrue(packer.getBytes()[0] == 104);
    }

    @Test
    public void testWriteByte() {
        BitStreamPacker packer = new BitStreamPacker();

        packer.writeByte(2);
        System.out.println(packer.getBytes()[0]);
        assertTrue(packer.getBytes()[0] == 2);

        packer.writeByte(3);
        System.out.println(packer.getBytes()[1]);
        byte[] data = packer.getBytes();
        assertTrue(data[0] == 2 && data[1] == 3);
    }
}
