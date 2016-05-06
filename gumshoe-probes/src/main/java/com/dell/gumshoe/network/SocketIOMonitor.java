package com.dell.gumshoe.network;

import static com.dell.gumshoe.util.Output.error;

import com.dell.gumshoe.hook.IoTraceListener.SocketListener;
import com.dell.gumshoe.io.IOMonitor;

import java.net.InetAddress;

/** monitor socket IO and report each as an event to registered listeners
 */
public class SocketIOMonitor extends IOMonitor implements SocketListener {
    private final AddressMatcher socketFilter;
    private boolean useNIOHooks;
    public SocketIOMonitor() {
        this(SubnetAddress.ANY, false, 500, Thread.MIN_PRIORITY, 1, false);
    }

    public SocketIOMonitor(AddressMatcher socketFilter, boolean useNIOHooks, int queueSize, int priority, int count, boolean statsEnabled) {
        this.socketFilter = socketFilter;
        this.useNIOHooks = useNIOHooks;
        setEventQueueSize(queueSize);
        setThreadCount(count);
        setThreadPriority(priority);
        setQueueStatisticsEnabled(statsEnabled);
    }

    @Override
    public void initializeProbe() throws Exception {
        if(useNIOHooks) {
            final String className = System.getProperty("java.nio.channels.spi.SelectorProvider");
            if( ! IoTraceSelectorProvider.class.getName().equals(className)) {
                error("System property must be set: java.nio.channels.spi.SelectorProvider.  NIO tracing will be incomplete");
            } else {
                IoTraceSelectorProvider.setSocketTraceEnabled(true);
            }
        }
        super.initializeProbe();
    }

    /////

    @Override
    public Object socketReadBegin() {
        return SocketReadEvent.begin();
    }

    @Override
    public Object socketWriteBegin() {
        return SocketWriteEvent.begin();
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
            final SocketEvent operation = (SocketEvent)context;
            operation.complete(address, port, bytes);
            queueEvent(operation);
        }
    }
}
