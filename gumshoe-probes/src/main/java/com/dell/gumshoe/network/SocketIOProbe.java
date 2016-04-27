package com.dell.gumshoe.network;

import com.dell.gumshoe.file.FileIOAccumulator;
import com.dell.gumshoe.io.IOAccumulator;
import com.dell.gumshoe.io.IOProbe;
import com.dell.gumshoe.io.IOMonitor;
import com.dell.gumshoe.stack.StackFilter;

import java.text.ParseException;
import java.util.Properties;

public class SocketIOProbe extends IOProbe {
    public static final String LABEL = "socket-io";

    public SocketIOProbe(ProbeServices services) {
        super(services);
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    protected IOMonitor createMonitor(Properties p) throws ParseException {
        final int handlerPriority = (int)getNumber(p, getPropertyName("handler.priority"), Thread.MIN_PRIORITY);
        final int handlerCount = (int)getNumber(p, getPropertyName("handler.count"), 1);
        final int queueSize = (int)getNumber(p, getPropertyName("handler.queue-size"), 500);
        final boolean includeNIO = isTrue(p, getPropertyName("use-nio-hooks"), false);
        final AddressMatcher[] acceptList = parseSocketMatchers(p.getProperty(getPropertyName("include")));
        final AddressMatcher[] rejectList = parseSocketMatchers(p.getProperty(getPropertyName("exclude"), "127.0.0.1/32:*"));
        final MultiAddressMatcher socketFilter = new MultiAddressMatcher(acceptList, rejectList);
        final boolean statsEnabled = isTrue(p, getPropertyName("handler.stats-enabled"), false);
        return new SocketIOMonitor(socketFilter, includeNIO, queueSize, handlerPriority, handlerCount, statsEnabled);
    }

    private static AddressMatcher[] parseSocketMatchers(String csv) throws ParseException {
        if(csv==null || csv.trim().equals("")) {
            return new AddressMatcher[0];
        }

        final String[] addressDescriptions = csv.split(",");
        final int len = addressDescriptions.length;
        final AddressMatcher[] matchers = new AddressMatcher[len];
        for(int i=0;i<len;i++) {
            matchers[i] = new SubnetAddress(addressDescriptions[i].trim());
        }
        return matchers;
    }

    @Override
    protected IOAccumulator createAccumulator(StackFilter filter) {
        return new SocketIOAccumulator(filter);
    }
}
