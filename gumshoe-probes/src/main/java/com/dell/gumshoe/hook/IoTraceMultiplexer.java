package com.dell.gumshoe.hook;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class IoTraceMultiplexer implements IoTraceListener {
    private final List<SocketListener> socketDelegates = new CopyOnWriteArrayList<>();
    private final List<DatagramListener> datagramDelegates = new CopyOnWriteArrayList<>();
    private final List<FileListener> fileDelegates = new CopyOnWriteArrayList<>();

    public void addDelegate(IoTraceListener delegate) {
        boolean hint = false;
        if(delegate instanceof SocketListener) {
            socketDelegates.add((SocketListener) delegate);
            hint = true;
        }
        if(delegate instanceof DatagramListener) {
            datagramDelegates.add((DatagramListener) delegate);
            hint = true;
        }
        if(delegate instanceof FileListener) {
            fileDelegates.add((FileListener) delegate);
            hint = true;
        }

        // marking interface is optional, if none was present listen for everything
        if( ! hint) {
            socketDelegates.add((SocketListener) delegate);
            datagramDelegates.add((DatagramListener) delegate);
            fileDelegates.add((FileListener) delegate);
        }
    }

    public void removeDelegate(IoTraceListener delegate) {
        socketDelegates.remove(delegate);
        datagramDelegates.remove(delegate);
        fileDelegates.remove(delegate);
    }

    @Override
    public Object socketReadBegin() {
        final Map<IoTraceListener,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceListener delegate : socketDelegates) {
            final Object memento = delegate.socketReadBegin();
            if(memento!=null) {
                mementoByDelegate.put(delegate, memento);
            }
        }
        return mementoByDelegate;
    }

    @Override
    public void socketReadEnd(Object context, InetAddress address, int port, int timeout, long bytesRead) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceListener delegate : socketDelegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.socketReadEnd(memento, address, port, timeout, bytesRead);
        }
    }

    @Override
    public Object socketWriteBegin() {
        final Map<IoTraceListener,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceListener delegate : socketDelegates) {
            final Object memento = delegate.socketWriteBegin();
            if(memento!=null) {
                mementoByDelegate.put(delegate, memento);
            }
        }
        return mementoByDelegate;
    }

    @Override
    public void socketWriteEnd(Object context, InetAddress address, int port, long bytesWritten) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceListener delegate : socketDelegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.socketWriteEnd(memento, address, port, bytesWritten);
        }
    }

    @Override
    public void socketReadEnd(Object context, SocketAddress address, long bytesRead) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceListener delegate : socketDelegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.socketReadEnd(memento, address, bytesRead);
        }

    }

    @Override
    public void socketWriteEnd(Object context, SocketAddress address, long bytesWritten) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceListener delegate : socketDelegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.socketWriteEnd(memento, address, bytesWritten);
        }

    }

    /////

    @Override
    public Object fileReadBegin(String path) {
        final Map<IoTraceListener,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceListener delegate : fileDelegates) {
            final Object memento = delegate.fileReadBegin(path);
            if(memento!=null) {
                mementoByDelegate.put(delegate, memento);
            }
        }
        return mementoByDelegate;
    }

    @Override
    public void fileReadEnd(Object context, long bytesRead) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceListener delegate : fileDelegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.fileReadEnd(memento, bytesRead);
        }
    }

    @Override
    public Object fileWriteBegin(String path) {
        final Map<IoTraceListener,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceListener delegate : fileDelegates) {
            final Object memento = delegate.fileWriteBegin(path);
            if(memento!=null) {
                mementoByDelegate.put(delegate, memento);
            }
        }
        return mementoByDelegate;
    }

    @Override
    public void fileWriteEnd(Object context, long bytesWritten) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceListener delegate : fileDelegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.fileWriteEnd(memento, bytesWritten);
        }
    }

    /////

    @Override
    public Object datagramReadBegin() {
        final Map<IoTraceListener,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceListener delegate : datagramDelegates) {
            final Object memento = delegate.datagramReadBegin();
            if(memento!=null) {
                mementoByDelegate.put(delegate, memento);
            }
        }
        return mementoByDelegate;
    }

    @Override
    public void datagramReadEnd(Object context, SocketAddress address, long bytesRead) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceListener delegate : datagramDelegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.datagramReadEnd(memento, address, bytesRead);
        }
    }

    @Override
    public Object datagramWriteBegin() {
        final Map<IoTraceListener,Object> mementoByDelegate = new IdentityHashMap<>();
        for(IoTraceListener delegate : datagramDelegates) {
            final Object memento = delegate.datagramWriteBegin();
            if(memento!=null) {
                mementoByDelegate.put(delegate, memento);
            }
        }
        return mementoByDelegate;
    }

    @Override
    public void datagramWriteEnd(Object context, SocketAddress address, long bytesWritten) {
        if( ! (context instanceof IdentityHashMap)) { return; }
        final Map mementoByDelegate = (Map)context;
        for(IoTraceListener delegate : datagramDelegates) {
            final Object memento = mementoByDelegate.get(delegate);
            delegate.datagramWriteEnd(memento, address, bytesWritten);
        }
    }
}
