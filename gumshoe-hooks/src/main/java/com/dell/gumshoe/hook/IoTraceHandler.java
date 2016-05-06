package com.dell.gumshoe.hook;

import java.net.InetAddress;
import java.net.SocketAddress;

public class IoTraceHandler {
    private static IoTraceListener NULL_OBJECT = new IoTraceAdapter();
    private static IoTraceListener DELEGATE = NULL_OBJECT;

    public synchronized static void addTrace(IoTraceListener delegate) throws Exception {
        if(DELEGATE==NULL_OBJECT) {
            DELEGATE = delegate;
        } else if(DELEGATE instanceof IoTraceMultiplexer) {
            final IoTraceMultiplexer multi = (IoTraceMultiplexer) DELEGATE;
            multi.addDelegate(delegate);
        } else {
            final IoTraceMultiplexer multi = new IoTraceMultiplexer();
            multi.addDelegate(DELEGATE);
            multi.addDelegate(delegate);
            DELEGATE = multi;
        }
    }

    public static void removeTrace(IoTraceListener delegate) throws Exception {
        if(DELEGATE==delegate) {
            DELEGATE = NULL_OBJECT;
        } else if(DELEGATE instanceof IoTraceMultiplexer) {
            final IoTraceMultiplexer multi = (IoTraceMultiplexer) DELEGATE;
            multi.removeDelegate(delegate);
        } else {
            throw new IllegalArgumentException("unable to remove, that IoTraceListener was not installed: " + delegate);
        }
    }

    public static IoTraceListener getTrace() {
        return DELEGATE;
    }

    /////

    public static Object socketReadBegin() {
        return DELEGATE.socketReadBegin();
    }

    public static void socketReadEnd(Object context, InetAddress address, int port,
            int timeout, long bytesRead) {
        DELEGATE.socketReadEnd(context, address, port, timeout, bytesRead);
    }

    public static Object socketWriteBegin() {
        return DELEGATE.socketWriteBegin();
    }

    public static void socketWriteEnd(Object context, InetAddress address, int port,
            long bytesWritten) {
        DELEGATE.socketWriteEnd(context, address, port, bytesWritten);
    }

    public static void socketReadEnd(Object context, SocketAddress address,
            long bytesRead) {
        DELEGATE.socketReadEnd(context, address, bytesRead);
    }

    public static void socketWriteEnd(Object context, SocketAddress address,
            long bytesWritten) {
        DELEGATE.socketWriteEnd(context, address, bytesWritten);
    }

    public static Object datagramReadBegin() {
        return DELEGATE.datagramReadBegin();
    }

    public static void datagramReadEnd(Object context, SocketAddress address,
            long bytesRead) {
        DELEGATE.datagramReadEnd(context, address, bytesRead);
    }

    public static Object datagramWriteBegin() {
        return DELEGATE.datagramWriteBegin();
    }

    public static void datagramWriteEnd(Object context, SocketAddress address,
            long bytesWritten) {
        DELEGATE.datagramWriteEnd(context, address, bytesWritten);
    }

    public static Object fileReadBegin(String path) {
        return DELEGATE.fileReadBegin(path);
    }

    public static void fileReadEnd(Object context, long bytesRead) {
        DELEGATE.fileReadEnd(context, bytesRead);
    }

    public static Object fileWriteBegin(String path) {
        return DELEGATE.fileWriteBegin(path);
    }

    public static void fileWriteEnd(Object context, long bytesWritten) {
        DELEGATE.fileWriteEnd(context, bytesWritten);
    }


}
