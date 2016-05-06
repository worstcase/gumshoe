package com.dell.gumshoe.hook;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/** adapter class to simplify implementation of IoTraceListener */
public class IoTraceAdapter implements IoTraceListener {
    public Object socketReadBegin() { return null; }
    public void socketReadEnd(Object context, InetAddress address, int port, int timeout, long bytesRead) { }
    public Object socketWriteBegin() { return null; }
    public void socketWriteEnd(Object context, InetAddress address, int port, long bytesWritten) { }
    public Object fileReadBegin(String path) { return null; }
    public void fileReadEnd(Object context, long bytesRead) { }
    public Object fileWriteBegin(String path) { return null; }
    public void fileWriteEnd(Object context, long bytesWritten) { }
    public Object datagramReadBegin() { return null; }
    public void datagramReadEnd(Object context, SocketAddress address, long bytesRead) { }
    public Object datagramWriteBegin() { return null; }
    public void datagramWriteEnd(Object context, SocketAddress address, long bytesWritten) { }

    // alt end methods for *some* NIO calls
    public void socketReadEnd(Object context, SocketAddress address, long bytesRead) {
        if(context!=null && address instanceof InetSocketAddress) {
            final InetSocketAddress ipAndPort = (InetSocketAddress)address;
            socketReadEnd(context, ipAndPort.getAddress(), ipAndPort.getPort(), 0, bytesRead);
        }
    }

    public void socketWriteEnd(Object context, SocketAddress address, long bytesWritten) {
        if(context!=null && address instanceof InetSocketAddress) {
            final InetSocketAddress ipAndPort = (InetSocketAddress)address;
            socketWriteEnd(context, ipAndPort.getAddress(), ipAndPort.getPort(), bytesWritten);
        }
    }
}