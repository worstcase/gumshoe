package com.dell.gumshoe.io;

import com.dell.gumshoe.IoTraceAdapter;
import com.dell.gumshoe.IoTraceUtil;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** monitor socket IO and report each as an event to registered listeners
 */
public class IOMonitor extends IoTraceAdapter {

    private BlockingQueue<IOEvent> queue;
    private final List<IOListener> listeners = new CopyOnWriteArrayList<>();

    private final AtomicInteger failCounter = new AtomicInteger();
    private final AtomicInteger successCounter = new AtomicInteger();
    private final EventConsumer consumer = new EventConsumer();
    private final Thread consumerThread = new Thread(consumer);

    private boolean enabled = true;
    private int eventQueueSize = 500;
    private long queueOverflowReportInterval = 300000;
    private long lastFailReport;

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

    public void setEventQueueSize(int eventQueueSize) {
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
        startConsumer();
        IoTraceUtil.addTrace(this);
    }

    public void destroyProbe() throws Exception {
        IoTraceUtil.removeTrace(this);
        stopConsumer();
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
        final boolean success = queue.offer(operation);

        if(success) {
            successCounter.incrementAndGet();
        } else {
            final int failCount = failCounter.incrementAndGet();
            final long now = System.currentTimeMillis();
            if(now - lastFailReport > queueOverflowReportInterval) {
                final int successCount = successCounter.get();
                lastFailReport = now;
                System.out.println(String.format("DIAG: IO tracing queue full (%d) for %d events out of %d",
                        eventQueueSize, failCount, failCount+successCount));
            }
        }
    }

    private void startConsumer() {
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private void stopConsumer() {
        consumer.shutdown();
        consumerThread.interrupt();
    }

    private class EventConsumer implements Runnable {
        private boolean keepRunning = true;

        public void shutdown() {
            if( ! keepRunning) throw new IllegalStateException("consumer was not running");
            keepRunning = false;
        }

        @Override
        public void run() {
            while(keepRunning) {
                try {
                    final IOEvent event = queue.take();
                    notifyListeners(event);
                } catch(InterruptedException ignore) { }
            }
        }
    }
}
