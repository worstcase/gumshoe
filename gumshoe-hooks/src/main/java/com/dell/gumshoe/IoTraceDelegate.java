package com.dell.gumshoe;

import java.net.InetAddress;

/** public interface for delegate to plug into sun.misc.IoTrace */
public interface IoTraceDelegate {
        public Object socketReadBegin();
        public void socketReadEnd(Object context, InetAddress address, int port, int timeout, long bytesRead);
        public Object socketWriteBegin();
        public void socketWriteEnd(Object context, InetAddress address, int port, long bytesWritten);
        public Object fileReadBegin(String path);
        public void fileReadEnd(Object context, long bytesRead);
        public Object fileWriteBegin(String path);
        public void fileWriteEnd(Object context, long bytesWritten);
}