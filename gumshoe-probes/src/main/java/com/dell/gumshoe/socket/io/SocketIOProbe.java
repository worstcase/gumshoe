package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.io.IOProbe;
import com.dell.gumshoe.io.IOMonitor;
import com.dell.gumshoe.socket.SocketMatcher;
import com.dell.gumshoe.socket.SocketMatcherSeries;
import com.dell.gumshoe.socket.SubnetAddress;

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
        final SocketMatcher[] acceptList = parseSocketMatchers(p.getProperty(getPropertyName("include")));
        final SocketMatcher[] rejectList = parseSocketMatchers(p.getProperty(getPropertyName("exclude"), "127.0.0.1/32:*"));
        final SocketMatcherSeries socketFilter = new SocketMatcherSeries(acceptList, rejectList);
        return new SocketIOMonitor(socketFilter);
    }

    private static SocketMatcher[] parseSocketMatchers(String csv) throws ParseException {
        if(csv==null || csv.trim().equals("")) {
            return new SocketMatcher[0];
        }

        final String[] addressDescriptions = csv.split(",");
        final int len = addressDescriptions.length;
        final SocketMatcher[] matchers = new SocketMatcher[len];
        for(int i=0;i<len;i++) {
            matchers[i] = new SubnetAddress(addressDescriptions[i].trim());
        }
        return matchers;
    }


}
