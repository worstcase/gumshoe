package com.dell.gumshoe;

import java.net.InetAddress;
import java.net.SocketAddress;

/** public interface for delegate to plug into sun.misc.IoTrace */
public interface IoTraceDelegate {
        public Object socketReadBegin();
        public void socketReadEnd(Object context, InetAddress address, int port, int timeout, long bytesRead);
        public Object socketWriteBegin();
        public void socketWriteEnd(Object context, InetAddress address, int port, long bytesWritten);

        public void socketReadEnd(Object context, SocketAddress address, long bytesRead);
        public void socketWriteEnd(Object context, SocketAddress address, long bytesWritten);

        public Object datagramReadBegin();
        public void datagramReadEnd(Object context, SocketAddress address, long bytesRead);
        public Object datagramWriteBegin();
        public void datagramWriteEnd(Object context, SocketAddress address, long bytesWritten);

        public Object fileReadBegin(String path);
        public void fileReadEnd(Object context, long bytesRead);
        public Object fileWriteBegin(String path);
        public void fileWriteEnd(Object context, long bytesWritten);
}