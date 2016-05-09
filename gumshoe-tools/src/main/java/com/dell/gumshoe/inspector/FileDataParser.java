package com.dell.gumshoe.inspector;

import com.dell.gumshoe.inspector.helper.DataTypeHelper;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** read saved gumshoe data from a file, possibly mixed with unrelated data
 *
 * two intended use cases:
 *
 * 1) navigate forwards and backwards
 *      fp = new FileParser(file);
 *      Map report = fp.getNextReport();
 *      Date time = fp.getReportTime();
 *      fp.close();
 *
 * 2) parse whole file and choose report:
 *      fp = new FileParser(file);
 *      fp.parseFile();
 *      Collection<Date> times = fp.getReportTimes();
 *      Date time = chooseOne( times );
 *      Map report = fp.Report(time);
 */
public class FileDataParser {
    private final BidirectionalMap savedPositions = new BidirectionalMap();
    private final RandomAccessFile raf;
    private Long lastPosition;
    private final String filename;
    private final Map<Long,String> typeByPosition = new HashMap<>();
    private boolean parsedWholeFile = false;

    public FileDataParser(String text) throws Exception {
        raf = new RandomAccessFile(text, "r");
        filename = text;
    }
    public FileDataParser(File file) throws Exception {
        raf = new RandomAccessFile(file, "r");
        filename = file.getName();
    }

    public void close() throws IOException {
        raf.close();
        savedPositions.clear();
        lastPosition = null;
    }

    /////

    /** scan entire file to find gumshoe reports */
    public void parseFile() throws Exception {
        while(read()!=null) { }
    }

    /** return all report times found so far (may be incomplete if parseFile() has not been called) */
    public Collection<Date> getReportTimes() {
        return savedPositions.getTimes();
    }

    /** return report collected at given time (may not find if parseFile() has not been called) */
    public Map<Stack, StatisticAdder> getReport(Date date) throws Exception {
        final Long position = savedPositions.getPosition(date);
        if(position==null) {
            lastPosition = null;
            return null;
        }
        raf.seek(position);
        return getNextReport();
    }

    /////

    /** return next report found after current position in file */
    public Map<Stack, StatisticAdder> getNextReport() throws Exception {
        long positionBefore = raf.getFilePointer();
        Map<Stack, StatisticAdder> report = read();
        if(report==null) {
            parsedWholeFile = true;
            lastPosition = null;
            raf.seek(0); // wrap around
        } else {
            lastPosition = savedPositions.getPositionAtOrAfter(positionBefore);
        }
        return report;
    }

    /** return report prior to current position in file */
    public Map<Stack, StatisticAdder> getPreviousReport() throws Exception {
        lastPosition = savedPositions.getPositionBefore(lastPosition);
        if(lastPosition==null) { return null; }
        raf.seek(lastPosition);
        return read();
    }

    /** return time of last report returned */
    public Date getReportTime() {
        return savedPositions.getTime(lastPosition);
    }

    public String getReportType() {
        return typeByPosition.get(lastPosition);
    }
    private Map<Stack,StatisticAdder> read() throws Exception {
        String line = null;

        // read until find a start tag
        long startPosition = raf.getFilePointer();
        Date startTime = null;
        String type = null;
        DataTypeHelper helper = null;
        while((line=raf.readLine())!=null) {
            startTime = ValueReporter.parseStartTagTime(line);
            if(startTime!=null) {
                type = ValueReporter.parseStartTagType(line);
                helper = DataTypeHelper.forType(type);
                break;
            }
            startPosition = raf.getFilePointer();
        }
        if(startTime==null) { return null; }

        final Map<Stack,StatisticAdder> out = new HashMap<>();
        StatisticAdder stats = null;
        final List<StackTraceElement> stackFrames = new ArrayList<>();
        while((line=raf.readLine())!=null) {
            if(line.startsWith(Stack.FRAME_PREFIX)) {
                if(stats==null) {
                    throw new IllegalStateException("stack line read but no stats");
                }
                stackFrames.add(Stack.parseFrame(line));
            } else if(ValueReporter.END_TAG.equals(line)) {
                if(stats!=null) {
                    final Stack stack = new Stack(stackFrames.toArray(new StackTraceElement[0]));
                    out.put(stack, stats);
                }
                savedPositions.put(startTime, startPosition);
                typeByPosition.put(startPosition, type);
                return out;
            } else {
                // parse first, make sure is expected format
                final StatisticAdder nextStats = helper.parse(line);
                if(stats!=null) {
                    final Stack stack = new Stack(stackFrames.toArray(new StackTraceElement[0]));
                    out.put(stack, stats);
                    stackFrames.clear();
                }
                stats = nextStats;
            }
        }
        throw new IllegalArgumentException("end of file before closing tag");
    }

    /** need to map in both directions; avoid looping over all entries */
    private static class BidirectionalMap {
        private final TreeMap<Date,Long> positionByTime = new TreeMap<>();
        private final TreeMap<Long,Date> timeByPosition = new TreeMap<>();

        public void put(Date time, Long position) {
            positionByTime.put(time, position);
            timeByPosition.put(position, time);
        }

        public void clear() {
            positionByTime.clear();
            timeByPosition.clear();
        }

        public Date getTime(Long position) {
            if(position==null) { return null; }
            return timeByPosition.get(position);
        }

        public Long getPosition(Date date) {
            return positionByTime.get(date);
        }

        public Collection<Date> getTimes() {
            return positionByTime.keySet();
        }

        public Long getPositionBefore(Long key) {
            if(timeByPosition.isEmpty()) { return null; }
            if(key==null) { return timeByPosition.lastKey(); }
            return timeByPosition.lowerKey(key);
        }

        public Long getPositionAtOrAfter(Long position) {
            if(timeByPosition.isEmpty()) { return null; }
            if(position==null) {
                return timeByPosition.firstKey();
            }
            if(timeByPosition.containsKey(position)) {
                return position;
            }
            return timeByPosition.higherKey(position);
        }
    }

    public String getFilename() {
        return filename;
    }

    public boolean hasPrevious() {
        return savedPositions.getPositionBefore(lastPosition)!=null;
    }

    public boolean hasNext() {
        return (lastPosition!=null && savedPositions.getPositionAtOrAfter(lastPosition+1)!=null) || ! parsedWholeFile;
    }
}