package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.stats.StatisticAdder;

public class SocketIODetailAdder extends IODetailAdder {
    @Override
    public String getType() {
        return ProbeManager.SOCKET_IO_LABEL;
    }

    @Override
    public StatisticAdder<IODetail> newInstance() {
        return new SocketIODetailAdder();
    }

    public static SocketIODetailAdder fromString(String line) {
        final SocketIODetailAdder out = new SocketIODetailAdder();
        try {
            out.setFromString(line);
        } catch(Exception e) {
            return null;
        }
        return out;
    }

}