package com.dell.gumshoe.file;

import com.dell.gumshoe.io.IOAccumulator;
import com.dell.gumshoe.io.IOProbe;
import com.dell.gumshoe.io.IOMonitor;
import com.dell.gumshoe.stack.StackFilter;

import java.text.ParseException;
import java.util.Properties;

public class FileIOProbe extends IOProbe {
    public static final String LABEL = "file-io";

    public FileIOProbe(ProbeServices services) {
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
        final FileMatcher[] acceptList = parseFileMatchers(p.getProperty(getPropertyName("include")));
        final FileMatcher[] rejectList = parseFileMatchers(p.getProperty(getPropertyName("exclude"), "**/gumshoe/**,*.jar,*.class"));
        final FileMatcherSeries socketFilter = new FileMatcherSeries(acceptList, rejectList);
        return new FileIOMonitor(socketFilter, queueSize, handlerPriority, handlerCount);
    }

    protected static FileMatcher[] parseFileMatchers(String csv) throws ParseException {
        if(csv==null || csv.trim().equals("")) {
            return new FileMatcher[0];
        }

        final String[] pathDescriptions = csv.split(",");
        final int len = pathDescriptions.length;
        final FileMatcher[] matchers = new FileMatcher[len];
        for(int i=0;i<len;i++) {
            matchers[i] = new PathPatternMatcher(pathDescriptions[i].trim());
        }
        return matchers;
    }

    @Override
    protected IOAccumulator createAccumulator(StackFilter filter) {
        return new FileIOAccumulator(filter);
    }
}
