package com.dell.gumshoe.network;

import com.dell.gumshoe.io.IOEvent;

import java.net.InetAddress;

public abstract class SocketEvent extends IOEvent {
    private static String convertAddress(InetAddress addr, int port) {
        final byte[] ip = addr.getAddress();
        return String.format("%d.%d.%d.%d:%d", 255&ip[0], 255&ip[1], 255&ip[2], 255&ip[3], port);
    }

    public void complete(InetAddress address, int port, long bytes) {
        complete(bytes);
        setTarget( convertAddress(address, port) );
    }
}
