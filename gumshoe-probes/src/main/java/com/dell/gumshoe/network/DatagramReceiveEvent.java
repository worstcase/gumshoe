package com.dell.gumshoe.network;

public class DatagramReceiveEvent extends DatagramEvent {
    @Override public boolean isRead() { return true; }
    @Override public boolean isWrite() { return false; }

}
