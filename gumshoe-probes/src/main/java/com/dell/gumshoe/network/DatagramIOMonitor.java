package com.dell.gumshoe.network;

import static com.dell.gumshoe.util.Output.error;

import com.dell.gumshoe.hook.IoTraceHandler;
import com.dell.gumshoe.hook.IoTraceListener;
import com.dell.gumshoe.hook.IoTraceListener.DatagramListener;
import com.dell.gumshoe.io.IOMonitor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.DatagramSocketImplFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class DatagramIOMonitor extends IOMonitor implements DatagramSocketImplFactory, DatagramListener {
    private IoTraceListener traceDelegate;
    private final Map<String,Method> methodByName = new HashMap<>();
    private final boolean useNIOHooks;
    private final AddressMatcher filter;
    private final Method delegateCreateImplMethod;
    private final boolean useMulticast;

    public DatagramIOMonitor(AddressMatcher filter, boolean useNIOHooks, boolean useMulticast, int queueSize, int priority, int count, boolean statsEnabled) throws Exception {
        this.filter = filter;
        this.useNIOHooks = useNIOHooks;
        this.useMulticast = useMulticast;
        setEventQueueSize(queueSize);
        setThreadCount(count);
        setThreadPriority(priority);
        setQueueStatisticsEnabled(statsEnabled);

        final Method[] methods = DatagramSocketImpl.class.getDeclaredMethods();
        for(Method method : methods) {
            if(Modifier.isProtected(method.getModifiers())) {
                method.setAccessible(true);
                methodByName.put(method.getName(), method);
            }
        }

        Class defaultFactoryClass = Class.forName("java.net.DefaultDatagramSocketImplFactory");
        delegateCreateImplMethod = defaultFactoryClass.getDeclaredMethod("createDatagramSocketImpl", Boolean.TYPE);
        delegateCreateImplMethod.setAccessible(true);
    }

    @Override
    public void initializeProbe() throws Exception {
        DatagramSocket.setDatagramSocketImplFactory(this);
        if(useNIOHooks) {
            final String className = System.getProperty("java.nio.channels.spi.SelectorProvider");
            if( ! IoTraceSelectorProvider.class.getName().equals(className)) {
                error("System property is not set: java.nio.channels.spi.SelectorProvider, NIO tracing will be incomplete");
            } else {
                IoTraceSelectorProvider.setDatagramTraceEnabled(true);
            }
        }
        super.initializeProbe();
    }

    /////

    @Override
    public Object datagramReadBegin() {
        return new DatagramReceiveEvent();
    }

    @Override
    public Object datagramWriteBegin() {
        return new DatagramSendEvent();
    }


    @Override
    public void datagramWriteEnd(Object context, SocketAddress address, long bytesWritten) {
        handleEvent(context, address, bytesWritten);
    }

    private void handleEvent(Object context, SocketAddress address, long bytes) {
        if(filter.matches(address)) {
            final DatagramEvent operation = (DatagramEvent)context;
            operation.complete(address, bytes);
            queueEvent(operation);
        }
    }
    /////

    public void destroyProbe() {
        // DatagramSocket.setDatagramSocketImplFactory() can be called only once
        throw new UnsupportedOperationException("datagram monitor cannot be removed");
    }

    @Override
    public DatagramSocketImpl createDatagramSocketImpl() {
        try {
            final DatagramSocketImpl impl = (DatagramSocketImpl) delegateCreateImplMethod.invoke(null, useMulticast);
            return new DatagramSocketWrapper(impl);
        } catch(Exception e) {
            throw new RuntimeException("failed to create datagram socket", e);
        }
    }

    private class DatagramSocketWrapper extends DatagramSocketImpl {
        private final DatagramSocketImpl delegate;

        public DatagramSocketWrapper(DatagramSocketImpl delegate) {
            this.delegate = delegate;
        }

        ///// invoke delegate methods directly
        public void setOption(int optID, Object value) throws SocketException { delegate.setOption(optID, value); }
        public int hashCode() { return delegate.hashCode(); }
        public Object getOption(int optID) throws SocketException { return delegate.getOption(optID); }
        public boolean equals(Object obj) { return delegate.equals(obj); }
        public String toString() { return delegate.toString(); }

        ///// invoke using reflection
        private Object invoke(String method, Object... args) throws IOException {
            try {
                return methodByName.get(method).invoke(delegate, args);
            } catch (Exception e) {
                rethrowIOException(e);
                return null;
            }
        }

        private void rethrowIOException(Throwable e) throws IOException {
            if(e instanceof RuntimeException) { throw (RuntimeException) e; }
            if(e instanceof IOException) { throw (IOException) e; }
            if(e.getCause()!=null && e.getCause()!=e) { rethrowIOException(e.getCause()); }
            throw new RuntimeException("failed to invoke method on delegate", e);
        }

        @Override
        protected void create() throws SocketException {
            try {
                invoke("create");
            } catch (IOException e) {
                if(e instanceof SocketException) { throw (SocketException) e; }
                throw new SocketException(e.getMessage());
            }
        }

        @Override
        protected void bind(int lport, InetAddress laddr) throws SocketException {
            try {
                invoke("bind", lport, laddr);
            } catch (IOException e) {
                if(e instanceof SocketException) { throw (SocketException) e; }
                throw new SocketException(e.getMessage());
            }
        }

        @Override
        protected int peek(InetAddress i) throws IOException {
            return (Integer) invoke("peek", i);
        }

        @Override
        protected int peekData(DatagramPacket p) throws IOException {
            return (Integer) invoke("peekData", p);
        }

        @Override
        protected void setTTL(byte ttl) throws IOException {
            invoke("setTTL", ttl);
        }

        @Override
        protected byte getTTL() throws IOException {
            return (Byte) invoke("getTTL");
        }

        @Override
        protected void setTimeToLive(int ttl) throws IOException {
            invoke("setTimeToLive", ttl);
        }

        @Override
        protected int getTimeToLive() throws IOException {
            return (Integer) invoke("getTimeToLive");
        }

        @Override
        protected void join(InetAddress inetaddr) throws IOException {
            invoke("join", inetaddr);
        }

        @Override
        protected void leave(InetAddress inetaddr) throws IOException {
            invoke("leave", inetaddr);
        }

        @Override
        protected void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
            invoke("joinGroup", mcastaddr, netIf);
        }

        @Override
        protected void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
            invoke("leaveGroup", mcastaddr, netIf);
        }

        @Override
        protected void close() {
            try {
                invoke("close");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ///// reflection and call IoTrace
        @Override
        protected void send(DatagramPacket p) throws IOException {
            final Object context = IoTraceHandler.datagramWriteBegin();
            invoke("send", p);
            IoTraceHandler.datagramWriteEnd(context, p.getSocketAddress(), p.getData().length);
        }

        @Override
        protected void receive(DatagramPacket p) throws IOException {
            final Object context = IoTraceHandler.datagramReadBegin();
            invoke("receive", p);
            IoTraceHandler.datagramReadEnd(context, p.getSocketAddress(), p.getData().length);
        }
    }
}
