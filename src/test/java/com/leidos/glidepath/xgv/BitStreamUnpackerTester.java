package com.leidos.glidepath.xgv;

import com.leidos.glidepath.appcommon.utils.BitStreamUnpacker;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by rushk1 on 11/6/2014.
 */
public class BitStreamUnpackerTester {
    @Ignore
    @Test
    public void testReadBit() {
        byte[] in = {1, 0};
        int bit;
        BitStreamUnpacker unpacker = new BitStreamUnpacker(in);

        for (int i = 0; i < 7; i++) {
            bit = unpacker.readBit();
            System.out.print(bit);
            assertTrue(bit == 0);
        }

        bit = unpacker.readBit();
        System.out.print(bit);
        assertTrue(bit == 1);
        System.out.print("\n");

        for (int i = 0; i < 8; i++) {
            bit = unpacker.readBit();
            System.out.print(bit);
            assertTrue(bit == 0);
        }
        System.out.println("\n");

        assertTrue(in[0] == 1 && in[1] == 0);
    }
}
