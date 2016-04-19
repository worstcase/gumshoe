package com.dell.gumshoe.file.io;

import com.dell.gumshoe.file.FileMatcherSeries;
import com.dell.gumshoe.file.PathPatternMatcher;
import com.dell.gumshoe.io.IOProbe;
import com.dell.gumshoe.io.IOMonitor;

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
        final FileMatcher[] acceptList = parseFileMatchers(p.getProperty(getPropertyName("include")));
        final FileMatcher[] rejectList = parseFileMatchers(p.getProperty(getPropertyName("exclude"), "**/gumshoe/**,*.jar,*.class"));
        final FileMatcherSeries socketFilter = new FileMatcherSeries(acceptList, rejectList);
        return new FileIOMonitor(socketFilter);
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


}
