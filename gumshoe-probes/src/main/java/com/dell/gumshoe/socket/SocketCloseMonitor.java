package com.dell.gumshoe.socket;

import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;

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
public class SocketCloseMonitor implements SocketImplFactory, SocketCloseMonitorMBean {
    private static AtomicInteger SOCKET_IDS = new AtomicInteger();
    
    private final AtomicInteger socketCount = new AtomicInteger();
    private final ConcurrentMap<Integer,SocketImplDecorator> openSockets = new ConcurrentHashMap<Integer,SocketImplDecorator>();
    private final ConcurrentMap<Stack,AtomicInteger> countByStack = new ConcurrentHashMap<>();
    private final Object clearClosedLock = new Object();
    private Thread clearClosedThread;
    private int clearClosedPerCount = 100;
    private StackFilter filter;
    private boolean enabled = true;
    
    private Constructor socketConstructor;
    private Method getSocketMethod;

    public SocketCloseMonitor() throws Exception { 
        Class<?> defaultSocketImpl = Class.forName("java.net.SocksSocketImpl");
        socketConstructor = defaultSocketImpl.getDeclaredConstructor();
        socketConstructor.setAccessible(true);
        
        getSocketMethod = SocketImpl.class.getDeclaredMethod("getSocket");
        getSocketMethod.setAccessible(true);
    }
    
    @Override
    public boolean isEnabled() { return enabled; }
    
    @Override
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
    
    public List<SocketImplDecorator> findOpenedBefore(Date cutoff) {
        final List<SocketImplDecorator> out = new ArrayList<SocketImplDecorator>();
        for(SocketImplDecorator value : openSockets.values()) {
            if(value.openTime.before(cutoff) && ! value.isClosed()) {
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
        final int socketId = SOCKET_IDS.incrementAndGet();
        if(enabled) {
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
        public final Date openTime;
        
        private SocketImplDecorator(int id) {
            this.id = id;
            this.impl = newSocketImpl();
            this.stack = new Stack();
            this.openTime = new Date();
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
                     openTime.toString(), 
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
    public String getReport(long minimumAge) {
        final long cutoff = System.currentTimeMillis() - minimumAge;
        final List<SocketImplDecorator> unclosed = findOpenedBefore(new Date(cutoff));
        final StringBuilder report = new StringBuilder();
        report.append("total ").append(unclosed.size()).append(" unclosed sockets:\n");
        
        final Map<Stack,Map<String,Integer>> summaryByStack = new HashMap<>();
        for(SocketImplDecorator wrapper : unclosed) {
            final Stack filteredStack = filter==null ? wrapper.stack : wrapper.stack.applyFilter(filter);
            Map<String,Integer> countByDesc = summaryByStack.get(filteredStack);
            if(countByDesc==null) {
                countByDesc = new HashMap<String,Integer>();
                summaryByStack.put(wrapper.stack, countByDesc);
            }
            final String address = wrapper.getSocket().getRemoteSocketAddress().toString();
            Integer count = countByDesc.get(address);
            countByDesc.put(address, (count==null) ? 1 : (count+1));
        }
        
        for(Map.Entry<Stack,Map<String,Integer>> stackEntry : summaryByStack.entrySet()) {
            final Stack stack = stackEntry.getKey();
            final Map<String,Integer> countByDescription = stackEntry.getValue();
            for(Map.Entry<String,Integer> countEntry : countByDescription.entrySet()) {
                report.append(countEntry.getValue()).append(" connections to ").append(countEntry.getKey()).append("\n");
            }
            report.append(stack).append("\n");
        }
        return report.toString();
    }
}
