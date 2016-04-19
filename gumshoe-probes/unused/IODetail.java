package com.dell.gumshoe.stats;

import com.dell.gumshoe.io.IOEvent;
import com.dell.gumshoe.socket.io.SocketIOMonitor;
import com.dell.gumshoe.socket.io.SocketIOMonitor.RW;

import java.net.InetAddress;

/** thread-safety: toString and fromString are synchronized on FORMAT,
 *      contention not likely in current usage so this is chosen
 *      to reduce overhead of creating lots of new MessageFormat instances.
 *      consider change if use-case is different.
 */
public class IODetail {
    private static String convertAddress(InetAddress addr, int port) {
        final byte[] ip = addr.getAddress();
        return String.format("%d.%d.%d.%d:%d", 255&ip[0], 255&ip[1], 255&ip[2], 255&ip[3], port);
    }

    final String address;
    final long readBytes;
    final long readTime;
    final long writeBytes;
    final long writeTime;

    public IODetail(IOEvent e) {
        this(convertAddress(e.getAddress(), e.getPort()), e.getReadBytes(), e.getReadElapsed(), e.getWriteBytes(), e.getWriteElapsed());
    }

    public IODetail(String address, long readBytes, long readTime, long writeBytes, long writeTime) {
        this.address = address;
        this.readBytes = readBytes;
        this.readTime = readTime;
        this.writeBytes = writeBytes;
        this.writeTime = writeTime;
    }

    @Override
    public String toString() {
        return String.format("%s: r %d bytes in %d ms, w %d bytes in %d ms",
            address, readBytes, readTime, writeBytes, writeTime );
    }
}