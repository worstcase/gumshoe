package com.dell.gumshoe.network;

import com.dell.gumshoe.io.IOEvent;

import java.net.SocketAddress;

public abstract class DatagramEvent extends IOEvent {

    public void complete(SocketAddress address, long bytes) {
        complete(bytes);
        setTarget( address.toString() );
    }

}
