package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.io.IOMonitor;
import com.dell.gumshoe.socket.SocketMatcher;
import com.dell.gumshoe.socket.SubnetAddress;

import java.net.InetAddress;

/** monitor socket IO and report each as an event to registered listeners
 */
public class SocketIOMonitor extends IOMonitor {
    private final SocketMatcher socketFilter;

    public SocketIOMonitor() {
        this(SubnetAddress.ANY);
    }

    public SocketIOMonitor(SocketMatcher socketFilter) {
        this.socketFilter = socketFilter;
    }

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
