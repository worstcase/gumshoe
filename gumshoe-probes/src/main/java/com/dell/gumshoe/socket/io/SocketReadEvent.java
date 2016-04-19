package com.dell.gumshoe.socket.io;


public class SocketReadEvent extends SocketEvent {
    public static SocketEvent begin() {
        return new SocketReadEvent();
    }

    private SocketReadEvent() { }

    @Override public boolean isRead() { return true; }
    @Override public boolean isWrite() { return false; }
}
