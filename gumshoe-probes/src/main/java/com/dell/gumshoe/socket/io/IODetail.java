package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.socket.io.SocketIOMonitor.Event;
import com.dell.gumshoe.socket.io.SocketIOMonitor.RW;

import java.net.InetAddress;
import java.text.MessageFormat;

public class IODetail {
    public static final MessageFormat FORMAT =
            new MessageFormat("{0,number} read ops {1,number} bytes in {2,number} ms, {3,number} write ops {4,number} bytes in {5,number} ms: {6}");

    private static String convertAddress(InetAddress addr, int port) {
        final byte[] ip = addr.getAddress();
        return String.format("%d.%d.%d.%d:%d", 255&ip[0], 255&ip[1], 255&ip[2], 255&ip[3], port);
    }

    final String address;
    final long readBytes;
    final long readTime;
    final long writeBytes;
    final long writeTime;
    final int readCount;
    final int writeCount;

    public IODetail(Event e) {
        this(convertAddress(e.getAddress(), e.getPort()), e.getReadBytes(), e.getReadElapsed(), e.getRw()==RW.READ?1:0, e.getWriteBytes(), e.getWriteElapsed(), e.getRw()==RW.WRITE?1:0);
    }

    public IODetail(String address, long readBytes, long readTime, long writeBytes, long writeTime) {
        this(address, readBytes, readTime, 1, writeBytes, writeTime, 1);
    }

    public IODetail(String address, long readBytes, long readTime, int readCount, long writeBytes, long writeTime, int writeCount) {
        this.address = address;
        this.readBytes = readBytes;
        this.readTime = readTime;
        this.writeBytes = writeBytes;
        this.writeTime = writeTime;
        this.readCount = readCount;
        this.writeCount = writeCount;
    }

    @Override
    public String toString() {
        synchronized(FORMAT) {
            return FORMAT.format(new Object[] { readCount, readBytes, readTime, writeCount, writeBytes, writeTime, address });
        }
    }
}