package com.dell.gumshoe.socket;

import com.dell.gumshoe.IoTraceAdapter;
import com.dell.gumshoe.IoTraceUtil;
import com.dell.gumshoe.stack.Filter;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;

import java.io.PrintStream;
import java.net.InetAddress;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** monitor socket IO and report each as an event to registered listeners
 */
public class SocketIOMonitor extends IoTraceAdapter implements SocketIOMBean {

    private BlockingQueue<Event> queue;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private final AtomicInteger failCounter = new AtomicInteger();
    private final AtomicInteger successCounter = new AtomicInteger();
    private final EventConsumer consumer = new EventConsumer();
    private final Thread consumerThread = new Thread(consumer);

    private final SocketMatcher socketFilter;
    private boolean enabled = true;
    private int eventQueueSize = 500;
    private long queueOverflowReportInterval = 300000;
    private long lastFailReport;

    public SocketIOMonitor() {
        this(SubnetAddress.ANY);
    }

    public SocketIOMonitor(SocketMatcher socketFilter) {
        this.socketFilter = socketFilter;
    }

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


    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public static interface Listener {
        public void socketIOHasCompleted(Event event);
    }

    private void notifyListeners(Event operation) {
        for(Listener listener : listeners) {
            try {
                listener.socketIOHasCompleted(operation);
            } catch(Exception ignore) {
                ignore.printStackTrace();
            }
        }
    }

    /////

    @Override
    public Object socketReadBegin() {
        return new Event(RW.READ);
    }

    @Override
    public Object socketWriteBegin() {
        return new Event(RW.WRITE);
    }

    @Override
    public void socketReadEnd(Object context, InetAddress address, int port, int timeout, long bytesRead) {
        handleEvent(context, address, port,  bytesRead);
    }

    @Override
    public void socketWriteEnd(Object context, InetAddress address, int port, long bytesWritten) {
        handleEvent(context, address, port, bytesWritten);
    }

    private void handleEvent(Object context, InetAddress address, int port, long bytes) {
        if(socketFilter.matches(address.getAddress(), port)) {
            final Event operation = (Event)context;
            operation.complete(address, port, bytes);
            queueEvent(operation);
        }
    }

    /////

    private void queueEvent(Event operation) {
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
                    final Event event = queue.take();
                    notifyListeners(event);
                } catch(InterruptedException ignore) { }
            }
        }
    }

    /////

    public enum RW { READ, WRITE };

    public static class Event {
        private final Stack stack;
        private final RW rw;
        private final long startTime;

        private InetAddress address;
        private int port;
        private long bytes;
        private long elapsed;

        public Event(RW rw) {
            this(rw, System.currentTimeMillis(), new Stack());
        }

        public Event(RW rw, long startTime, Stack stack) {
            this.rw = rw;
            this.startTime = startTime;
            this.stack = stack;
        }

        private void complete(InetAddress address, int port, long bytes) {
            this.elapsed = System.currentTimeMillis() - startTime;
            this.address = address;
            this.port = port;
            this.bytes = bytes;
        }

        public Stack getStack() { return stack; }
        public RW getRw() { return rw; }
        public long getReadElapsed() { return rw==RW.READ ? elapsed : 0; }
        public long getWriteElapsed() { return rw==RW.WRITE ? elapsed : 0; }
        public InetAddress getAddress() { return address; }
        public int getPort() { return port; }
        public long getReadBytes() { return rw==RW.READ ? bytes : 0; }
        public long getWriteBytes() { return rw==RW.WRITE ? bytes : 0; }

        @Override
        public String toString() {
            return String.format("%s:%d %d bytes %s in %d ms\n%s",
                    address, port, bytes, rw.toString(), elapsed, stack.toString());
        }
    }
}
