package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.stats.StatisticAdder;

import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SocketIODetailAdder extends IODetailAdder {
    public static SocketIODetailAdder fromString(String line) throws ParseException {
        final SocketIODetailAdder out = new SocketIODetailAdder();
        out.add(IODetail.fromString(line));
        return out;
    }

    @Override
    public String getType() {
        return Probe.SOCKET_IO_LABEL;
    }

    @Override
    public StatisticAdder<IODetail> newInstance() {
        return new SocketIODetailAdder();
    }
}