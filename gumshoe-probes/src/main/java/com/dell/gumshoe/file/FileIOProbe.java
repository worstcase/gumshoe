package com.dell.gumshoe.file;

import com.dell.gumshoe.io.IOAccumulator;
import com.dell.gumshoe.io.IOMonitor;
import com.dell.gumshoe.io.IOProbe;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.util.Configuration;

import java.text.ParseException;

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
    protected IOMonitor createMonitor(Configuration p) throws ParseException {
        final int handlerPriority = (int)p.getNumber("handler.priority", Thread.MIN_PRIORITY);
        final int handlerCount = (int)p.getNumber("handler.count", 1);
        final int queueSize = (int)p.getNumber("handler.queue-size", 500);
        final FileMatcher[] acceptList = parseFileMatchers(p.getProperty("include"));
        final FileMatcher[] rejectList = parseFileMatchers(p.getProperty("exclude", "**/gumshoe/**,*.jar,*.class"));
        final FileMatcherSeries socketFilter = new FileMatcherSeries(acceptList, rejectList);
        final boolean statsEnabled = p.isTrue("handler.stats-enabled", false);
        return new FileIOMonitor(socketFilter, queueSize, handlerPriority, handlerCount, statsEnabled);
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
