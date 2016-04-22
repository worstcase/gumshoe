package com.dell.gumshoe.socket;

import com.dell.gumshoe.network.SubnetAddress;

import java.text.ParseException;

import junit.framework.TestCase;

public class TestSubnetMatcher extends TestCase {
    public void testParseValid() throws ParseException {
        String[] valid = {
                "127.0.0.1/24:1234",
                "127.0.0.1/20:1234",
                "127.0.0.1/8:1",
                "127.0.0.1/32:*",
                "255.255.255.0/0:0",
                "0.0.0.255/32:65535"
        };
        
        byte b = (byte)255;
        int i=(int)b;
        System.out.println("255: " + i);
        for(String desc : valid) {
            System.out.println(desc + ": " + new SubnetAddress(desc).toString());
        }
    }
    
    public void testMatch() throws Exception {
        assertTrue(new SubnetAddress("123.45.67.89/24:1234").matches(new byte[] { 123, 45, 67, 22 }, 1234));
        assertFalse(new SubnetAddress("123.45.67.89/24:1234").matches(new byte[] { 123, 45, 77, 22 }, 1234));
        assertFalse(new SubnetAddress("123.45.67.89/24:1234").matches(new byte[] { 123, 45, 67, 22 }, 1235));
        assertTrue(new SubnetAddress("123.45.67.89/24:*").matches(new byte[] { 123, 45, 67, 22 }, 1235));
        assertTrue(new SubnetAddress("123.45.67.89/20:1234").matches(new byte[] { 123, 45, 77, 22 }, 1234));
    }
}
