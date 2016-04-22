package com.dell.gumshoe.network;

import com.dell.gumshoe.io.IOMonitor;
import com.dell.gumshoe.io.IoTraceSelectorProvider;

import java.net.InetAddress;

/** monitor socket IO and report each as an event to registered listeners
 */
public class SocketIOMonitor extends IOMonitor {
    private final AddressMatcher socketFilter;
    private boolean useNIOHooks;
    public SocketIOMonitor() {
        this(SubnetAddress.ANY, false);
    }

    public SocketIOMonitor(AddressMatcher socketFilter, boolean useNIOHooks) {
        this.socketFilter = socketFilter;
        this.useNIOHooks = useNIOHooks;
    }

    @Override
    public void initializeProbe() throws Exception {
        if(useNIOHooks) {
            final String className = System.getProperty("java.nio.channels.spi.SelectorProvider");
            if( ! IoTraceSelectorProvider.class.getName().equals(className)) {
                System.out.println(
                          "WARNING: NIO tracing will be incomplete\n"
                        + "         System property must be set: java.nio.channels.spi.SelectorProvider\n"
                        + "         See gumshoe documentation for details");
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
