package com.dell.gumshoe.network;

import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.StackStatisticSource;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/** monitor and report on sockets left open
 */
public class SocketCloseMonitor implements SocketImplFactory, StackStatisticSource<UnclosedStats> {
    private static AtomicInteger SOCKET_IDS = new AtomicInteger();

    private final AtomicInteger socketCount = new AtomicInteger();
    private final ConcurrentMap<Integer,SocketImplDecorator> openSockets = new ConcurrentHashMap<Integer,SocketImplDecorator>();
    private final ConcurrentMap<Stack,AtomicInteger> countByStack = new ConcurrentHashMap<>();
    private final Object clearClosedLock = new Object();
    private Thread clearClosedThread;
    private int clearClosedPerCount = 100;
    private StackFilter filter;
    private boolean enabled = true;
    private long minReportingAge;

    private Constructor socketConstructor;
    private Method getSocketMethod;

    public SocketCloseMonitor(long minAge, StackFilter filter) throws Exception {
        Class<?> defaultSocketImpl = Class.forName("java.net.SocksSocketImpl");
        socketConstructor = defaultSocketImpl.getDeclaredConstructor();
        socketConstructor.setAccessible(true);

        getSocketMethod = SocketImpl.class.getDeclaredMethod("getSocket");
        getSocketMethod.setAccessible(true);

        this.minReportingAge = minAge;
        this.filter = filter;
    }

    public void setFilter(StackFilter filter) {
        this.filter = filter;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if( ! enabled) {
            synchronized(clearClosedLock) {
                clearClosedSockets();
                openSockets.clear();
                countByStack.clear();
            }
        }
    }

    /////

    public void initializeProbe() throws IOException {
        Socket.setSocketImplFactory(this);
    }

    public void destroyProbe() {
        // Socket.setSocketImplFactory() can be called only once
        throw new UnsupportedOperationException("socket close monitor cannot be removed");
    }

    /** request the system clear closed sockets every Nth time a new socket is created */
    public void setClearClosedSocketsInterval(int numberOfSockets) {
        clearClosedPerCount = numberOfSockets;
        if(clearClosedThread==null) {
            clearClosedThread = new Thread(new ScanForClosed());
            clearClosedThread.setDaemon(true);
            clearClosedThread.setName("clear-closed-sockets");
            clearClosedThread.start();
        }
    }

    public int getClearClosedSocketsInterval() {
        return clearClosedPerCount;
    }

    public List<SocketImplDecorator> findOpenedBefore(long cutoff) {
        final List<SocketImplDecorator> out = new ArrayList<SocketImplDecorator>();
        for(SocketImplDecorator value : openSockets.values()) {
            if(value.openTime<cutoff && ! value.isClosed()) {
                out.add(value);
            }
        }
        return out;
    }

    public int getSocketCount() {
        clearClosedSockets();
        return socketCount.get();
    }

    public Map<Stack, Integer> getCountsByStack() {
        final Map<Stack, Integer> out = new HashMap<>(countByStack.size());
        for(Map.Entry<Stack,AtomicInteger> entry : countByStack.entrySet()) {
            out.put(entry.getKey(), entry.getValue().get());
        }
        return out;
    }

    /////

    /** JVM hook: capture info about socket as it is created */
    @Override
    public SocketImpl createSocketImpl() {
        if(enabled) {
            final int socketId = SOCKET_IDS.incrementAndGet();
            if(socketId%clearClosedPerCount==0) {
                notifyClearClosed();
            }
            final SocketImplDecorator wrapper = new SocketImplDecorator(socketId);
            openSockets.put(wrapper.id, wrapper);
            socketCount.incrementAndGet();
            AtomicInteger countWithThisStack = countByStack.get(wrapper.stack);
            if(countWithThisStack==null) {
                final AtomicInteger newCount = new AtomicInteger();
                final AtomicInteger priorEntry = countByStack.putIfAbsent(wrapper.stack, newCount);
                countWithThisStack = priorEntry==null ? newCount : priorEntry;
            }
            countWithThisStack.incrementAndGet();
            // return raw impl, but keep a ref to track it
            return wrapper.impl;
        } else {
            // not monitoring?  return raw untracked impl
            return newSocketImpl();
        }
    }

    private SocketImpl newSocketImpl() {
        try {
            return (SocketImpl) socketConstructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** information maintained about each open socket */
    public class SocketImplDecorator {
        public final int id;
        public final SocketImpl impl;
        public final Stack stack;
        public final long openTime;

        private SocketImplDecorator(int id) {
            this.id = id;
            this.impl = newSocketImpl();
            this.stack = new Stack().applyFilter(filter);
            this.openTime = System.currentTimeMillis();
        }

        private Socket getSocket() {
            try {
                return (Socket) getSocketMethod.invoke(impl);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private boolean isClosed() {
            return getSocket().isClosed();
        }

//        public SocketImplDecorator filterStack(StackFilter filter) {
//            return new SocketImplDecorator(id, impl, stack.applyFilter(filter), openTime);
//        }

        @Override
        public String toString() {
            final Socket socket = getSocket();

            return String.format("%s %s (%s)\n%s",
                     socket.getRemoteSocketAddress().toString(),
                     new Date(openTime).toString(),
                     socket.isClosed() ? "closed" : "open",
                     stack.toString());
        }
    }

    private void clearClosedSockets() {
        synchronized(clearClosedLock) {
            for(SocketImplDecorator value : openSockets.values()) {
                if(value.isClosed()) {
                    socketCount.decrementAndGet();
                    openSockets.remove(value.id);
                }
            }
        }
    }

    /////

    private void notifyClearClosed() {
        synchronized(clearClosedLock) {
            clearClosedLock.notifyAll();
        }
    }

    private class ScanForClosed implements Runnable {
        @Override
        public void run() {
            synchronized(clearClosedLock) {
                while(true) {
                    try {
                        clearClosedLock.wait();
                        clearClosedSockets();
                    } catch(Exception ignore) { }
                }
            }
        }
    }

    /////

    @Override
    public void reset() { /*no-op*/ }

    @Override
    public Map<Stack,UnclosedStats> getStats() {
        return getStats(minReportingAge);
    }

    Map<Stack,UnclosedStats> getStats(long minAge) {
        final long now = System.currentTimeMillis();
        final long cutoff = now - minAge;
        final List<SocketImplDecorator> unclosed = findOpenedBefore(cutoff);
        final Map<Stack,UnclosedStats> summaryByStack = new HashMap<>();
        for(SocketImplDecorator socket : unclosed) {
            UnclosedStats stats = summaryByStack.get(socket.stack);
            if(stats==null) {
                stats = new UnclosedStats();
                summaryByStack.put(socket.stack, stats);
            }
            stats.add(now, socket);
        }
        return summaryByStack;
    }
}
