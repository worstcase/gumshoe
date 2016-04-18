package com.dell.gumshoe.thread;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;

/** information recorded about a single thread */
public class ThreadDetails {
    private final long cpuTime; // nanos
    private final long userTime; // nanos
    private final long blockedCount;
    private final long blockedTime;
    private final long waitedCount;
    private final long waitedTime;
    private final State state;

    public ThreadDetails(long cpuTime, long userTime, ThreadInfo item) {
        this.cpuTime = cpuTime;
        this.userTime = userTime;
        this.blockedCount = item.getBlockedCount();
        this.blockedTime = item.getBlockedTime();
        this.waitedCount = item.getWaitedCount();
        this.waitedTime = item.getWaitedTime();
        this.state = item.getThreadState();
    }

    public ThreadDetails(long cpuTime, long userTime, long blockedCount, long blockedTime, long waitedCount, long waitedTime, State state) {
        this.cpuTime = cpuTime;
        this.userTime = userTime;
        this.blockedCount = blockedCount;
        this.blockedTime = blockedTime;
        this.waitedCount = waitedCount;
        this.waitedTime = waitedTime;
        this.state = state;
    }

    public long getCpuTime() { return cpuTime; }
    public long getUserTime() { return userTime; }
    public long getBlockedCount() { return blockedCount; }
    public long getBlockedTime() { return blockedTime; }
    public long getWaitedCount() { return waitedCount; }
    public long getWaitedTime() { return waitedTime; }
    public State getState() { return state; }
}
