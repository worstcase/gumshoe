package com.dell.gumshoe.network;

import java.net.SocketAddress;

public class MultiAddressMatcher implements AddressMatcher {
    private final AddressMatcher[] acceptList;
    private final AddressMatcher[] rejectList;

    public MultiAddressMatcher(AddressMatcher[] acceptList, AddressMatcher[] rejectList) {
        this.acceptList = acceptList;
        this.rejectList = rejectList;
    }

    @Override
    public boolean matches(byte[] addressBytes, int port) {
        for(AddressMatcher accept : acceptList) {
            if(accept.matches(addressBytes, port)) {
                return true;
            }
        }

        for(AddressMatcher reject : rejectList) {
            if(reject.matches(addressBytes, port)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean matches(SocketAddress address) {
        for(AddressMatcher accept : acceptList) {
            if(accept.matches(address)) {
                return true;
            }
        }

        for(AddressMatcher reject : rejectList) {
            if(reject.matches(address)) {
                return false;
            }
        }

        return true;
    }

}
