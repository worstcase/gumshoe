package com.dell.gumshoe.network;

import com.dell.gumshoe.io.IOAccumulator;
import com.dell.gumshoe.io.IOMonitor;
import com.dell.gumshoe.io.IOProbe;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.util.Configuration;

import java.text.ParseException;

public class DatagramIOProbe extends IOProbe {
    public static final String LABEL = "datagram-io";

    public DatagramIOProbe(ProbeServices services) {
        super(services);
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    protected IOMonitor createMonitor(Configuration cfg) throws Exception {
        final int handlerPriority = (int)cfg.getNumber("handler.priority", Thread.MIN_PRIORITY);
        final int handlerCount = (int)cfg.getNumber("handler.count", 1);
        final int queueSize = (int)cfg.getNumber("handler.queue-size", 500);
        final boolean includeNIO = cfg.isTrue("use-nio-hooks", false);
        final AddressMatcher[] acceptList = parseSocketMatchers(cfg.getProperty("include"));
        final AddressMatcher[] rejectList = parseSocketMatchers(cfg.getProperty("exclude", "127.0.0.1/32:*"));
        final MultiAddressMatcher socketFilter = new MultiAddressMatcher(acceptList, rejectList);
        final boolean statsEnabled = cfg.isTrue("handler.stats-enabled", false);

        // TODO: detect OS; print warning if not set and OS is windows
        final String usingOnly = cfg.getProperty("using-only", "unicast");
        final boolean useMulticast = "multicast".equals(usingOnly);

        return new DatagramIOMonitor(socketFilter, includeNIO, useMulticast, queueSize, handlerPriority, handlerCount, statsEnabled);
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
        return new DatagramIOAccumulator(filter);
    }
}
