package com.dell.gumshoe.io;

import static com.dell.gumshoe.util.Output.error;

import com.dell.gumshoe.hook.IoTraceAdapter;
import com.dell.gumshoe.hook.IoTraceHandler;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** monitor socket IO and report each as an event to registered listeners
 */
public class IOMonitor extends IoTraceAdapter {

    private boolean enabled = true;

    private BlockingQueue<IOEvent> queue;
    private final List<IOListener> listeners = new CopyOnWriteArrayList<>();

    private boolean queueStatsEnabled;
    private int eventQueueSize = 500;
    private long queueOverflowReportInterval = 300000;
    private long lastFailReport;
    private final AtomicInteger emptyCounter = new AtomicInteger();
    private final AtomicInteger failCounter = new AtomicInteger();
    private final AtomicInteger successCounter = new AtomicInteger();
    private final AtomicLong sumSize = new AtomicLong();
    private final AtomicInteger maxSize = new AtomicInteger();

    private final List<EventConsumer> consumers = new CopyOnWriteArrayList<>();
    private final AtomicInteger consumerCount = new AtomicInteger();
    private int threadCount = 1;
    private int threadPriority = Thread.MIN_PRIORITY;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if( ! enabled) {
            queue.clear();
        }
    }

    public int getEventQueueSize() {
        return eventQueueSize;
    }

    public synchronized void setEventQueueSize(int eventQueueSize) {
        if(queue!=null) { throw new IllegalStateException("cannot resize queue after probe has been installed"); }
        this.eventQueueSize = eventQueueSize;
    }

    public long getOverflowReportInterval() {
        return queueOverflowReportInterval;
    }

    public void setOverflowReportInterval(long queueOverflowReportInterval) {
        this.queueOverflowReportInterval = queueOverflowReportInterval;
    }

    public void initializeProbe() throws Exception {
        queue = new LinkedBlockingQueue<>(eventQueueSize);
        startConsumers();
        IoTraceHandler.addTrace(this);
    }

    public void destroyProbe() throws Exception {
        IoTraceHandler.removeTrace(this);
        setThreadCount(0);
        queue = null;
    }

    /////

    public void addListener(IOListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(IOEvent operation) {
        for(IOListener listener : listeners) {
            try {
                listener.ioHasCompleted(operation);
            } catch(Exception ignore) {
                ignore.printStackTrace();
            }
        }
    }

    /////

    protected void queueEvent(IOEvent operation) {
        if( ! enabled) { return; }

        final int size = queueStatsEnabled ? queue.size() : 0;

        final boolean success = queue.offer(operation);

        if(queueStatsEnabled) {
            if(size==0) {
                emptyCounter.incrementAndGet();
            }
            sumSize.addAndGet(size);
            updateMax(size);
            if(success) {
                successCounter.incrementAndGet();
            } else {
                failCounter.incrementAndGet();
            }
        }

        if( ! success) {
            final long now = System.currentTimeMillis();
            if(now - lastFailReport > queueOverflowReportInterval) {
                lastFailReport = now;
                error(String.format("GUMSHOE: IO event queue for %s is full", getClass().getSimpleName()));
            }
        }
    }

    // http://stackoverflow.com/questions/6072040
    private void updateMax(int size) {
        while(true) {
            final int currentMax = maxSize.get();
            // no need to update
            if (currentMax >= size) { return; }

            // check if another thread updated first
            final boolean setSuccessful = maxSize.compareAndSet(currentMax, size);
            if (setSuccessful) { break; }

            // another thread did, start over
        }
    }

    public int getFailureCount() {
        return failCounter.get();
    }

    public int getSuccessCount() {
        return successCounter.get();
    }

    public String getQueueStats() {
        if(queueStatsEnabled) {
            final int success = successCounter.get();
            final int failure = failCounter.get();
            final int total = success+failure;
            final int empty = emptyCounter.get();
            final float emptyPercent = (100f * empty) / total;
            final float fullPercent = (100f * failure) / total;
            final float avg = sumSize.get() / total;
            return String.format("%d events: depth avg %.0f, max %d, empty %d (%.0f%%), dropped %d (%.0f%%)",
                    total, avg, maxSize.get(), empty, emptyPercent, failure, fullPercent);
        } else {
            return "queue statistics are not enabled";
        }
    }

    public void setQueueStatisticsEnabled(boolean enabled) {
        this.queueStatsEnabled = enabled;
    }

    public boolean isQueueStatisticsEnabled() {
        return this.queueStatsEnabled;
    }

    public void resetQueueCounters() {
        successCounter.set(0);
        failCounter.set(0);
        maxSize.set(0);
        sumSize.set(0);
    }

    public void setThreadCount(int count) {
        if(this.threadCount!=count) {
            synchronized(consumers) {
                final boolean increase = count > this.threadCount;
                this.threadCount = count;
                if(increase) { startConsumers(); }
                else { stopConsumers(); }
            }
        }
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadPriority(int priority) {
        this.threadPriority = priority;
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    private String createThreadName() {
        return getClass().getSimpleName() + "-event-" + consumerCount.incrementAndGet();
    }
    private void startConsumers() {
        synchronized(consumers) {
            while(consumers.size()<threadCount) {
                final EventConsumer consumer = new EventConsumer();
                consumer.start();
            }
        }
    }

    private void stopConsumers() {
        synchronized(consumers) {
            final int count = consumers.size()-threadCount;
            for(int i=0;i<count;i++) {
                consumers.get(i).shutdown();
            }
        }
    }

    private class EventConsumer extends Thread {
        private boolean keepRunning = true;

        public EventConsumer() {
            setPriority(threadPriority);
            setName(createThreadName());
            setDaemon(true);
        }

        public void start() {
            super.start();
            consumers.add(EventConsumer.this);
        }

        public void shutdown() {
            if( ! keepRunning) throw new IllegalStateException("consumer was not running");
            keepRunning = false;
            interrupt();
        }

        @Override
        public void run() {
            while(keepRunning) {
                try {
                    final IOEvent event = queue.take();
                    notifyListeners(event);
                } catch(InterruptedException ignore) { }
            }
            consumers.remove(EventConsumer.this);
        }
    }
}
