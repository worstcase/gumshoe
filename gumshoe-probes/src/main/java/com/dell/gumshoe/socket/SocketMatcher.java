package com.dell.gumshoe.socket;

public interface SocketMatcher {
    boolean matches(byte[] addressBytes, int port);
}
