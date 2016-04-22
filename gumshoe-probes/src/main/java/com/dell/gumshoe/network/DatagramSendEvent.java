package com.dell.gumshoe.network;

public class DatagramSendEvent extends DatagramEvent {

    @Override public boolean isRead() { return false; }
    @Override public boolean isWrite() { return true; }

}
