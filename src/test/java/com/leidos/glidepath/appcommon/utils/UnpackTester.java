package com.leidos.glidepath.appcommon.utils;

import com.leidos.glidepath.gps.NioUtils;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;


public class UnpackTester {

    private UnpackUtils utils = UnpackUtils.getInstance();

    @Test
    public void unpack16()   {
        byte[] twoBytes = new byte[2];
        twoBytes[0] = 0x00;
        twoBytes[1] = 0x01;

        int value = utils.unpack16(twoBytes);
        assertTrue(value == 256);

        twoBytes = new byte[2];
        twoBytes[0] = (byte) 0xFF;
        twoBytes[1] = (byte) 0xFF;

        value = utils.unpack16(twoBytes);
        assertTrue(value == -1);

    }


    @Test
    public void unpackU16()   {
        byte[] twoBytes = new byte[2];
        twoBytes[0] = 0x00;
        twoBytes[1] = 0x01;

        int value = utils.unpackU16(twoBytes);
        assertTrue(value == 256);

        twoBytes = new byte[2];
        twoBytes[0] = (byte) 0xFF;
        twoBytes[1] = (byte) 0xFF;

        value = utils.unpackU16(twoBytes);
        assertTrue(value == 65535);

    }


    @Test
    public void unpack32()   {
        byte[] fourBytes = new byte[4];
        fourBytes[0] = 0x00;
        fourBytes[1] = 0x01;
        fourBytes[2] = 0x00;
        fourBytes[3] = (byte) 0xF1;


        int value = utils.unpack32(fourBytes);
        assertTrue(value == -251657984);

        fourBytes = new byte[4];
        fourBytes[0] = 0x01;
        fourBytes[1] = 0x00;
        fourBytes[2] = 0x00;
        fourBytes[3] = 0x00;

        value = utils.unpack32(fourBytes);
        assertTrue(value == 1);

    }

    @Test
    public void unpackU32()   {
        byte[] fourBytes = new byte[4];
        fourBytes[0] = 0x00;
        fourBytes[1] = 0x01;
        fourBytes[2] = 0x00;
        fourBytes[3] = (byte) 0xF1;


        long value = utils.unpackU32(fourBytes);
        assertTrue(value == 4043309312L);

        fourBytes = new byte[4];
        fourBytes[0] = 0x01;
        fourBytes[1] = 0x00;
        fourBytes[2] = 0x00;
        fourBytes[3] = 0x00;

        value = utils.unpackU32(fourBytes);
        assertTrue(value == 1L);

    }

    @Test
    public void unpack8Test()   {
        byte theByte = 0x01;

        int value = utils.unpack8(theByte);
        assertTrue(value == 1);


        theByte = (byte) 0xFF;
        value = utils.unpack8(theByte);
        assertTrue(value == -1);

    }


    @Test
    public void unpackU8Test()   {
        byte theByte = 0x01;

        int value = utils.unpackU8(theByte);
        assertTrue(value == 1);

        theByte = (byte) 0xFF;
        value = utils.unpackU8(theByte);
        assertTrue(value == 255);
    }

}
