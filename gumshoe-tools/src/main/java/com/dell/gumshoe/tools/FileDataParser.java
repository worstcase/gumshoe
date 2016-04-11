package com.dell.gumshoe.tools;

import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.tools.stats.DataTypeHelper;

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
 *      Map sample = fp.getNextSample();
 *      Date time = fp.getSampleTime();
 *      fp.close();
 *
 * 2) parse whole file and choose sample:
 *      fp = new FileParser(file);
 *      fp.parseFile();
 *      Collection<Date> times = fp.getSampleTimes();
 *      Date time = chooseOne( times );
 *      Map sample = fp.getSample(time);
 */
public class FileDataParser {
    private final BidirectionalMap savedPositions = new BidirectionalMap();
    private final RandomAccessFile file;
    private Long lastPosition;
    private final ValueReporter reporter = new ValueReporter("socket-io");
    public FileDataParser(RandomAccessFile file) { this.file = file; }

    public FileDataParser(String text) throws Exception {
        this(new RandomAccessFile(text, "r"));
    }

    public void close() throws IOException {
        file.close();
        savedPositions.clear();
        lastPosition = null;
    }

    /////

    /** scan entire file to find gumshoe samples */
    public void parseFile() throws Exception {
        while(read()!=null) { }
    }

    /** return all sample times found so far (may be incomplete if parseFile() has not been called) */
    public Collection<Date> getSampleTimes() {
        return savedPositions.getTimes();
    }

    /** return sample collected at given time (may not find if parseFile() has not been called) */
    public Map<Stack, StatisticAdder> getSample(Date date) throws Exception {
        final Long position = savedPositions.getPosition(date);
        if(position==null) {
            lastPosition = null;
            return null;
        }
        file.seek(position);
        return getNextSample();
    }

    /////

    /** return next sample found after current position in file */
    public Map<Stack, StatisticAdder> getNextSample() throws Exception {
        long positionBefore = file.getFilePointer();
        Map<Stack, StatisticAdder> sample = read();
        if(sample==null) {
            lastPosition = null;
            file.seek(0); // wrap around
        } else {
            lastPosition = savedPositions.getPositionAtOrAfter(positionBefore);
        }
        return sample;
    }

    /** return sample prior to current position in file */
    public Map<Stack, StatisticAdder> getPreviousSample() throws Exception {
        lastPosition = savedPositions.getPositionBefore(lastPosition);
        if(lastPosition==null) { return null; }
        file.seek(lastPosition);
        return read();
    }

    /** return time of last sample returned */
    public Date getSampleTime() {
        return savedPositions.getTime(lastPosition);
    }

    private Map<Stack,StatisticAdder> read() throws Exception {
        String line = null;

        // read until find a start tag
        long startPosition = file.getFilePointer();
        Date startTime = null;
        String type = null;
        DataTypeHelper helper = null;
        while((line=file.readLine())!=null) {
            startTime = ValueReporter.parseStartTagTime(line);
            if(startTime!=null) {
                type = ValueReporter.parseStartTagType(line);
                helper = DataTypeHelper.forType(type);
                break;
            }
            startPosition = file.getFilePointer();
        }
        if(startTime==null) { return null; }

        final Map<Stack,StatisticAdder> out = new HashMap<>();
        StatisticAdder stats = null;
        final List<StackTraceElement> stackFrames = new ArrayList<>();
        while((line=file.readLine())!=null) {
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
            if(key==null) { return timeByPosition.lastKey(); }
            return timeByPosition.lowerKey(key);
        }

        public Long getPositionAtOrAfter(Long position) {
            if(position==null) {
                return timeByPosition.firstKey();
            }
            if(timeByPosition.containsKey(position)) {
                return position;
            }
            return timeByPosition.higherKey(position);
        }
    }
}