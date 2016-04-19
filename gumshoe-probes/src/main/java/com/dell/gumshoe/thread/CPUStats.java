package com.dell.gumshoe.thread;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.stats.StatisticAdder;

import java.lang.Thread.State;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

/** combined CPU usage for multiple threads */
public class CPUStats implements StatisticAdder<ThreadDetails> {
    private static final MessageFormat FORMAT = new MessageFormat(
            "{0,number,#} ns cpu:"
            + " {1,number,#} ns user,"
            + " {2,number,#} ms {3,number,#} block,"
            + " {4,number,#} ms {5,number,#} wait,"
            + " threads: {6}");

    private final TallyMap countByState = new TallyMap();
    private long blockedCount;
    private long waitedCount;
    private long blockedTime;
    private long waitedTime;
    private long userTime;
    private long cpuTime;

    public long getCpuTime() { return cpuTime; }
    public long getUserTime() { return userTime; }
    public long getBlockedCount() { return blockedCount; }
    public long getBlockedTime() { return blockedTime; }
    public long getWaitedCount() { return waitedCount; }
    public long getWaitedTime() { return waitedTime; }

    public int getThreadCount() {
        int sum = 0;
        for(Integer count : countByState.values()) {
            sum += count;
        }
        return sum;
    }

    public int getThreadCount(State state) {
        return countByState.get(state);
    }

    @Override
    public void add(StatisticAdder<ThreadDetails> o) {
        final CPUStats value = (CPUStats)o;
        countByState.add(value.countByState);
        blockedCount += value.blockedCount;
        blockedTime += value.blockedTime;
        waitedCount += value.waitedCount;
        waitedTime += value.waitedTime;
        userTime += value.userTime;
        cpuTime += value.cpuTime;
    }

    @Override
    public void add(ThreadDetails value) {
        countByState.increment(value.getState());
        blockedCount += value.getBlockedCount();
        blockedTime += Math.max(0L, value.getBlockedTime());
        waitedCount += value.getWaitedCount();
        waitedTime += Math.max(0L, value.getWaitedTime());
        userTime += value.getUserTime();
        cpuTime += Math.max(0L, value.getCpuTime());
    }

    @Override
    public StatisticAdder<ThreadDetails> newInstance() {
        return new CPUStats();
    }

    @Override
    public String getType() {
        return ProbeManager.CPU_USAGE_LABEL;
    }

    /////

    @Override
    public String toString() {
        final Object[] arg = {
                cpuTime, userTime, blockedTime, blockedCount, waitedTime, waitedCount,
                countByState.toString()
        };
        synchronized(FORMAT) {
            return FORMAT.format(arg);
        }
    }

    public static CPUStats fromString(String line) {
        final Object[] fields;
        try {
            synchronized(FORMAT) {
                fields = FORMAT.parse(line);
            }
        } catch(ParseException e) {
            return null;
        }

        final CPUStats out = new CPUStats();
        out.cpuTime = ((Number)fields[0]).longValue();
        out.userTime = ((Number)fields[1]).longValue();
        out.blockedTime = ((Number)fields[2]).longValue();
        out.blockedCount = ((Number)fields[3]).longValue();
        out.waitedTime = ((Number)fields[4]).longValue();
        out.waitedCount = ((Number)fields[5]).longValue();
        out.countByState.fromString((String)fields[6]);
        return out;
    }

    /////

    /** map with functions to increment value, add values from another map, format to/from string */
    private static class TallyMap extends EnumMap<State,Integer> {
        public TallyMap() {
            super(State.class);
        }

        public Integer get(State key) {
            final Integer orig = super.get(key);
            return orig==null ? 0 : orig;
        }

        public void increment(State key) {
            final int sum = get(key)+1;
            put(key, sum);
        }

        public void add(TallyMap that) {
            for(State key : State.values()) {
                final int sum = get(key)+that.get(key);
                put(key, sum);
            }
        }

        @Override
        public String toString() {
            final StringBuilder out = new StringBuilder();
            for(Map.Entry<State,Integer> entry : entrySet()) {
                if(out.length()>0) { out.append(", "); }
                out.append(entry.getValue()).append(" ").append(entry.getKey());
            }
            return out.toString();
        }

        public void fromString(String stringValue) {
            final TallyMap out = new TallyMap();
            for(String entryPart : stringValue.split(", +")) {
                final String[] fieldParts = entryPart.split(" ");
                final int value = Integer.parseInt(fieldParts[0]);
                final State key = State.valueOf(fieldParts[1]);
                put(key, value);
            }
        }
    }
}
