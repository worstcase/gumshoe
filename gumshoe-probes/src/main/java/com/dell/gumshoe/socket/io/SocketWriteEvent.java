package com.dell.gumshoe.socket.io;


public class SocketWriteEvent extends SocketEvent {
    public static SocketEvent begin() {
        return new SocketWriteEvent();
    }

    private SocketWriteEvent() { }

    @Override public boolean isRead() { return false; }
    @Override public boolean isWrite() { return true; }
}
