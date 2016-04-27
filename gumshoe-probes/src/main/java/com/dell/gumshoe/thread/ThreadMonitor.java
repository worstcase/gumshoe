package com.dell.gumshoe.thread;

import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.StackStatisticSource;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * from MXBean report Map<Thread,Info> where Info = { stack, cpu stats }
 * store prior cpu stats, report delta
 */
public class ThreadMonitor implements StackStatisticSource<CPUStats> {
    private Map<Stack,CPUStats> usageStatistics = new ConcurrentHashMap<>();
    private LongDiffMap cpuTimeById = new LongDiffMap();
    private LongDiffMap userTimeById = new LongDiffMap();
    private LongDiffMap blockedCountById = new LongDiffMap();
    private LongDiffMap blockedTimeById = new LongDiffMap();
    private LongDiffMap waitCountById = new LongDiffMap();
    private LongDiffMap waitTimeById = new LongDiffMap();

    private long lastDumpTime;
    private long nextDumpTime;
    private ThreadDumper dumper;
    private Thread thread;
    private long effectiveInterval;

    private boolean enabled;
    private StackFilter stackFilter = StackFilter.NONE;
    private ThreadFilter threadFilter = ThreadFilter.NONE;
    private long dumpInterval = 5000;
    private float jitter;
    private int threadPriority = Thread.MIN_PRIORITY;
    private int dumpCount;
    private long dumpTimeSum;

    public long getJitter() {
        return (long)jitter*dumpInterval;
    }

    public void setJitter(long jitter) {
        setDumpInterval(getDumpInterval(), jitter);
    }

    public long getDumpInterval() { return dumpInterval; }

    public void setDumpInterval(long dumpInterval) {
        setDumpInterval(dumpInterval, getJitter());
    }

    private void setDumpInterval(long dumpInterval, long jitter) {
        if(dumper==null) {
            this.dumpInterval = this.effectiveInterval = dumpInterval;
            this.jitter = ((float)jitter)/dumpInterval; // store as ratio so can apply to effective interval
        } else {
            synchronized(dumper) {
                this.dumpInterval = this.effectiveInterval = dumpInterval;
                this.jitter = ((float)jitter)/dumpInterval;
                this.lastDumpTime = System.currentTimeMillis();
                this.nextDumpTime = this.lastDumpTime + dumpInterval + (long) (dumpInterval*jitter*(Math.random()-0.5));
                this.thread.interrupt();
            }
        }
    }

    public int getThreadPriority() { return threadPriority; }
    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
        if(thread!=null) {
            thread.setPriority(threadPriority);
        }
    }

    public long getEffectiveInterval() { return effectiveInterval; }
    public void setStackFilter(StackFilter stackFilter) { this.stackFilter = stackFilter; }
    public void setThreadFilter(ThreadFilter threadFilter) { this.threadFilter = threadFilter; }

    public void setContentionMonitoringEnabled(boolean enable) {
        final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        if(enabled && ! mbean.isThreadContentionMonitoringSupported()) {
            throw new UnsupportedOperationException("JVM does not support contention monitoring");
        }
        mbean.setThreadContentionMonitoringEnabled(enable);
    }

    public boolean isContentionMonitoringEnabled() {
        final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        return mbean.isThreadContentionMonitoringEnabled();
    }

    /////

    public void initializeProbe() throws Exception {
        setEnabled(true);
    }

    public void destroyProbe() throws Exception {
        setEnabled(false);
    }

    public synchronized void setEnabled(boolean enabled) {
        if(enabled==this.enabled) { return; }
        this.enabled = enabled;
        this.effectiveInterval = dumpInterval;
        if(enabled) {
            startThread();
        } else {
            stopThread();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getAverageDumpTime() {
        return dumpTimeSum / dumpCount;
    }
    /////

    @Override
    public Map<Stack,CPUStats> getStats() {
        return Collections.unmodifiableMap(usageStatistics);
    }

    @Override
    public void reset() {
        usageStatistics.clear();
    }

    /////    public ThreadDetails(long cpuTime, long blockedCount, long blockedCount, long blockedTime, long waitedCount, long waitedTime, State state) {


    /** add thread dump to the running tallies */
    private void recordThreadInfo() throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[] items = mbean.dumpAllThreads(false, false);
        for(ThreadInfo item : items) {
            final long id = item.getThreadId();
            final long cpuTimeTotal = mbean.getThreadCpuTime(id);
            final Long cpuDelta = cpuTimeById.diff(id, cpuTimeTotal);
            final long userTimeTotal = mbean.getThreadUserTime(id);
            final Long userTimeDelta = userTimeById.diff(id, userTimeTotal);
            final long blockedCountTotal = item.getBlockedCount();
            final Long blockedCountDelta = blockedCountById.diff(id, blockedCountTotal);
            final long blockedTimeTotal = Math.max(0L, item.getBlockedTime());
            final Long blockedTimeDelta = blockedTimeById.diff(id, blockedTimeTotal);
            final long waitCountTotal = item.getWaitedCount();
            final Long waitCountDelta = waitCountById.diff(id, waitCountTotal);
            final long waitTimeTotal = Math.max(0L, item.getWaitedTime());
            final Long waitTimeDelta = waitTimeById.diff(id, waitTimeTotal);
            final ThreadDetails stat = new ThreadDetails(cpuDelta, userTimeDelta, blockedCountDelta, blockedTimeDelta, waitCountDelta, waitTimeDelta, item.getThreadState());

            final Stack filteredStack = new Stack(item.getStackTrace()).applyFilter(stackFilter);
            if(threadFilter.useThread(filteredStack, stat)) {
                recordThreadInfo(filteredStack, stat);
            }
        }
        final long elapsed = System.currentTimeMillis() - startTime;
        dumpTimeSum += elapsed;
        dumpCount++;
    }

    /** add info on one thread to the running tally */
    private void recordThreadInfo(Stack stack, ThreadDetails info) {
        CPUStats tally = usageStatistics.get(stack);
        if(tally==null) {
            tally = new CPUStats();
            usageStatistics.put(stack, tally);
        }
        tally.add(info);
    }

    /////

    private void startThread() {
        if(thread!=null) {
            throw new IllegalStateException("monitor is already running");
        }

        dumper = new ThreadDumper();
        thread = new Thread(dumper);
        thread.setName("gumshoe-cpu-monitor");
        thread.setDaemon(true);
        thread.setPriority(threadPriority);
        thread.start();
    }

    private void stopThread() {
        if(thread!=Thread.currentThread()) {
            int count = 0;
            while(thread.isAlive() && count++ < 5) {
                thread.interrupt();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) { }
            }
            if(thread.isAlive()) {
                throw new IllegalStateException("monitor thread will not die");
            }
        } // current thread calling disableDueToException() will stop b/c enabled = false
        thread = null;
    }

    private synchronized void disableDueToException() {
        setEnabled(false);
    }

    public class ThreadDumper implements Runnable {
        private int exceptionCountSinceLastSuccess;
        private int tooLongCountSinceLastSuccess;
        private int successCountSinceLastTooLong;
        private long shortSinceLastNotShort;

        @Override
        public void run() {
            while(enabled) {
                try {
                    waitForDumpTime();
                    performDumpAndReschedule();
                } catch(InterruptedException e) {
                    // enabled or nextDumpTime should have changed
                }
            }
        }

        private void waitForDumpTime() throws InterruptedException {
            // trying to make sure the boolean test and Thread.sleep share a single
            // access to nextDumpTime in case it was changed from another thread
            long sleepTime = nextDumpTime - System.currentTimeMillis();
            while(sleepTime>0) {
                Thread.sleep(sleepTime);
                sleepTime = nextDumpTime - System.currentTimeMillis();
            }
        }

        private void performDumpAndReschedule() throws InterruptedException {
            final long potentialLastDumpTime = System.currentTimeMillis();

            try {
                recordThreadInfo();
                lastDumpTime = potentialLastDumpTime;

                exceptionCountSinceLastSuccess = 0;
            } catch(Exception e) {
                if(shouldDisableReporting()) {
                    disableDueToException();
                }
                exceptionCountSinceLastSuccess++;
                return;
            }

            calcualteNextDumpTime();
        }

        private synchronized void calcualteNextDumpTime() {
            final long elapsed = System.currentTimeMillis() - lastDumpTime;

            // dump took longer than reporting frequency? delay next report
            if(elapsed > effectiveInterval) {
                tooLongCountSinceLastSuccess++;
                maybeSlowReporting();
                successCountSinceLastTooLong = 0;

                nextDumpTime = System.currentTimeMillis() + effectiveInterval + (long)(effectiveInterval*jitter*(Math.random()-0.5));
            } else {
                successCountSinceLastTooLong++;
                maybeSpeedReporting(elapsed);
                tooLongCountSinceLastSuccess = 0;

                nextDumpTime = lastDumpTime + effectiveInterval + (long)(effectiveInterval*jitter*(Math.random()-0.5));
            }
        }

        private boolean shouldDisableReporting() {
            return exceptionCountSinceLastSuccess>=4;
        }

        private void maybeSlowReporting() {
            if(tooLongCountSinceLastSuccess>=3) {
                // happening often enough, need to reduce rate
                effectiveInterval *= 2;
            }
        }

        private void maybeSpeedReporting(long elapsed) {
            if(effectiveInterval==dumpInterval) { return; }
            if(successCountSinceLastTooLong<10) { return; }

            if(elapsed < effectiveInterval/4) {
                shortSinceLastNotShort++;
            } else {
                shortSinceLastNotShort=0;
            }
            if(shortSinceLastNotShort<10) { return; }

            // can report faster
            shortSinceLastNotShort = 0;
            effectiveInterval = Math.max(effectiveInterval/2, dumpInterval);
        }
    }

    private static class LongDiffMap extends HashMap<Long,Long> {
        public Long diff(Long key, Long newValue) {
            final Long oldValue = super.get(key);
            // newValue<oldValue if old thread dies and ID is reused
            final boolean firstValueThisThread = oldValue==null || newValue<oldValue;
            final long diff = firstValueThisThread ? newValue : (newValue-oldValue);
            put(key, newValue);
            return diff;
        }
    }
}
