package com.dell.gumshoe.network;

import java.net.SocketAddress;

public interface AddressMatcher {
    boolean matches(byte[] addressBytes, int port);
    boolean matches(SocketAddress address);
}
