package com.dell.gumshoe.network;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.MessageFormat;
import java.text.ParseException;

public class SubnetAddress implements AddressMatcher {
    public static final AddressMatcher ANY = new MatchAny();

    private final byte[] targetAddress = new byte[4];
    private final byte[] mask = new byte[4];
    private final Integer targetPort;

    public SubnetAddress(String description) throws ParseException {
        final MessageFormat format = new MessageFormat("{0,number,integer}.{1,number,integer}.{2,number,integer}.{3,number,integer}/{4,number,integer}:{5}");
        final Object[] fields = format.parse(description);
        for(int i=0;i<4;i++) {
            final Number addressByte = (Number)fields[i];
            if(addressByte.longValue()<0 || addressByte.longValue()>255) {
                throw new ParseException("invalid IP/mask:port value: " + description,i);
            }
            targetAddress[i] = addressByte.byteValue();
        }

        final int bits = ((Number)fields[4]).byteValue();
        long bitvalue = 0xFFFFFFFF ^ ((1L<<(32-bits)) - 1);
        mask[0] = (byte) (bitvalue>>24 & 0xFF);
        mask[1] = (byte) (bitvalue>>16 & 0xFF);
        mask[2] = (byte) (bitvalue>>8 & 0xFF);
        mask[3] = (byte) (bitvalue & 0xFF);

        final String portField = (String)fields[5];
        if(portField.equals("*")) {
            targetPort = null;
        } else {
            targetPort = Integer.parseInt(portField);
            if(targetPort<0 || targetPort>65535) {
                throw new ParseException("invalid port: " + description, 0);
            }
        }
    }

    @Override
    public boolean matches(byte[] addressBytes, int port) {
        for(int i=0;i<4;i++) {
            if(((addressBytes[i]^targetAddress[i]) & mask[i]) > 0) {
                return false;
            }
        }

        return targetPort==null || targetPort.equals(port);
    }

    public boolean matches(SocketAddress address) {
        if(address instanceof InetSocketAddress) {
            final InetSocketAddress ipAndPort = (InetSocketAddress)address;
            return matches(ipAndPort.getAddress().getAddress(), ipAndPort.getPort());
        } else {
            // ipv6 or some esoteric network
            return false;
        }

    }

    public String toString() {
        final StringBuilder out = new StringBuilder();
        for(int i=0;i<4;i++) {
            if(i>0) out.append(".");
            out.append(0xFF & targetAddress[i]);
        }
        out.append("/");
        for(int i=0;i<4;i++) {
            if(i>0) out.append(".");
            out.append(0xFF & mask[i]);
        }
        out.append(":");
        if(targetPort==null) out.append("*");
        else out.append(targetPort);
        return out.toString();
    }

    private static class MatchAny implements AddressMatcher {
        public boolean matches(byte[] unused, int port) { return true; }
        public boolean matches(SocketAddress address) { return true; }
    }
}
