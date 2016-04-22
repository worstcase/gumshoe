package com.dell.gumshoe;

import sun.misc.IoTrace;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class IoTraceMultiplexer implements IoTraceDelegate {
    private List<IoTraceDelegate> delegates = new CopyOnWriteArrayList<>();

    public void addDelegate(IoTraceDelegate delegate) {
        delegates.add(delegate);
    }

    public void removeDelegate(IoTraceDelegate delegate) {
        delegates.remove(delegate);
    }

    @Override
    public Object socketReadBegin() {
        final Map<IoTraceDelegate,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceDelegate delegate : delegates) {
            mementoByDelegate.put(delegate, delegate.socketReadBegin());
        }
        return mementoByDelegate;
    }

    @Override
    public void socketReadEnd(Object context, InetAddress address, int port, int timeout, long bytesRead) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceDelegate delegate : delegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.socketReadEnd(memento, address, port, timeout, bytesRead);
        }
    }

    @Override
    public Object socketWriteBegin() {
        final Map<IoTraceDelegate,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceDelegate delegate : delegates) {
            mementoByDelegate.put(delegate, delegate.socketWriteBegin());
        }
        return mementoByDelegate;
    }

    @Override
    public void socketWriteEnd(Object context, InetAddress address, int port, long bytesWritten) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceDelegate delegate : delegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.socketWriteEnd(memento, address, port, bytesWritten);
        }
    }

    @Override
    public Object fileReadBegin(String path) {
        final Map<IoTraceDelegate,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceDelegate delegate : delegates) {
            mementoByDelegate.put(delegate, delegate.fileReadBegin(path));
        }
        return mementoByDelegate;
    }

    @Override
    public void fileReadEnd(Object context, long bytesRead) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceDelegate delegate : delegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.fileReadEnd(memento, bytesRead);
        }
    }

    @Override
    public Object fileWriteBegin(String path) {
        final Map<IoTraceDelegate,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceDelegate delegate : delegates) {
            mementoByDelegate.put(delegate, delegate.fileWriteBegin(path));
        }
        return mementoByDelegate;
    }

    @Override
    public void fileWriteEnd(Object context, long bytesWritten) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceDelegate delegate : delegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.fileWriteEnd(memento, bytesWritten);
        }
    }

    /////

    public static void install(IoTraceDelegate delegate) throws Exception {
        final Field nullField = IoTrace.class.getField("NULL_OBJECT");
        nullField.setAccessible(true);
        final Object nullObject = nullField.get(IoTrace.class);

        final Field delegateField = IoTrace.class.getField("delegate");
        delegateField.setAccessible(true);
        final Object oldValue = delegateField.get(IoTrace.class);
        if(oldValue==nullObject) {
            delegateField.set(IoTrace.class, delegate);
        } else if(oldValue instanceof IoTraceMultiplexer) {
            final IoTraceMultiplexer multi = (IoTraceMultiplexer) oldValue;
            multi.addDelegate(delegate);
        } else {
            final IoTraceMultiplexer multi = new IoTraceMultiplexer();
            multi.addDelegate((IoTraceDelegate)oldValue);
            delegateField.set(IoTrace.class, multi);
        }
    }

    public static void remove(IoTraceDelegate delegate) throws Exception {
        final Field delegateField = IoTrace.class.getField("delegate");
        delegateField.setAccessible(true);
        final Object oldValue = delegateField.get(IoTrace.class);
        if(oldValue.equals(delegate)) {
            final Field nullField = IoTrace.class.getField("NULL_OBJECT");
            nullField.setAccessible(true);
            final Object nullObject = nullField.get(IoTrace.class);

            delegateField.set(IoTrace.class, nullObject);
        } else if(oldValue instanceof IoTraceMultiplexer) {
            final IoTraceMultiplexer multi = (IoTraceMultiplexer) oldValue;
            multi.removeDelegate(delegate);
        } else {
            throw new IllegalArgumentException("unable to remove, IoTraceDelegate was not installed: " + delegate);
        }
    }

    @Override
    public void socketReadEnd(Object context, SocketAddress address, long bytesRead) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceDelegate delegate : delegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.socketReadEnd(memento, address, bytesRead);
        }

    }

    @Override
    public void socketWriteEnd(Object context, SocketAddress address, long bytesWritten) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceDelegate delegate : delegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.socketWriteEnd(memento, address, bytesWritten);
        }

    }

    @Override
    public Object datagramReadBegin() {
        final Map<IoTraceDelegate,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceDelegate delegate : delegates) {
            mementoByDelegate.put(delegate, delegate.datagramReadBegin());
        }
        return mementoByDelegate;
    }

    @Override
    public void datagramReadEnd(Object context, SocketAddress address, long bytesRead) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceDelegate delegate : delegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.datagramReadEnd(memento, address, bytesRead);
        }
    }

    @Override
    public Object datagramWriteBegin() {
        final Map<IoTraceDelegate,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceDelegate delegate : delegates) {
            mementoByDelegate.put(delegate, delegate.datagramWriteBegin());
        }
        return mementoByDelegate;
    }

    @Override
    public void datagramWriteEnd(Object context, SocketAddress address, long bytesWritten) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceDelegate delegate : delegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.datagramWriteEnd(memento, address, bytesWritten);
        }
    }
}
