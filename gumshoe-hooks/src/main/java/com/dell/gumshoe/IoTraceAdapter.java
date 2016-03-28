package com.dell.gumshoe;

import java.net.InetAddress;

/** adapter class to simplify implementation of IoTraceDelegate */
public class IoTraceAdapter implements IoTraceDelegate {
    public Object socketReadBegin() { return null; }
    public void socketReadEnd(Object context, InetAddress address, int port, int timeout, long bytesRead) { }
    public Object socketWriteBegin() { return null; }
    public void socketWriteEnd(Object context, InetAddress address, int port, long bytesWritten) { }
    public Object fileReadBegin(String path) { return null; }
    public void fileReadEnd(Object context, long bytesRead) { }
    public Object fileWriteBegin(String path) { return null; }
    public void fileWriteEnd(Object context, long bytesWritten) { }
}