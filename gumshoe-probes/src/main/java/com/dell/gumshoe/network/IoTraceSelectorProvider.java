package com.dell.gumshoe.network;

import com.dell.gumshoe.hook.IoTraceHandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/** to support NIO monitoring, set system property:
 *      <tt>java.nio.channels.spi.SelectorProvider</tt>
 *  to the name of this class
 */
public class IoTraceSelectorProvider extends sun.nio.ch.SelectorProviderImpl {
    // if false, use wrapper; if true, leave unmonitored
    private static boolean usePlainServerSocketChannel = true;
    private static boolean usePlainSocketChannel = true;
    private static boolean usePlainDatagramChannel = true;

    public static void setSocketTraceEnabled(boolean enabled) {
        usePlainServerSocketChannel = false;
        usePlainSocketChannel = false;
    }

    public static boolean isSocketTraceEnabled() {
        if(usePlainServerSocketChannel!=usePlainSocketChannel) {
            throw new IllegalStateException("IoTraceSelectorProvider socket and ServerSocket are inconsistent");
        }
        return usePlainServerSocketChannel;
    }

    public static void setDatagramTraceEnabled(boolean enabled) {
        usePlainDatagramChannel = false;
    }

    public static boolean isDatagramTraceEnabled() {
        return usePlainDatagramChannel;
    }

    private final SelectorProvider delegate;
    private final Method closeChannelMethod;
    private final Method closeSelectorMethod;
    private final Method configureBlockingMethod;
    private final Method registerMethod;


    public IoTraceSelectorProvider() throws Exception {
        // choose delegate that would have been used if gumshoe wasn't here
        final SelectorProvider providerAsService = getProviderAsService();
        this.delegate = providerAsService==null ? sun.nio.ch.DefaultSelectorProvider.create() : providerAsService;

        closeChannelMethod = AbstractSelectableChannel.class.getDeclaredMethod("implCloseSelectableChannel");
        closeChannelMethod.setAccessible(true);
        closeSelectorMethod = AbstractSelector.class.getDeclaredMethod("implCloseSelector");
        closeSelectorMethod.setAccessible(true);
        configureBlockingMethod = AbstractSelectableChannel.class.getDeclaredMethod("implConfigureBlocking", Boolean.TYPE);
        configureBlockingMethod.setAccessible(true);
        registerMethod = AbstractSelector.class.getDeclaredMethod("register", AbstractSelectableChannel.class, Integer.TYPE, Object.class);
        registerMethod.setAccessible(true);
    }

    /**  check service definitions from jars, if there is one and it isn't gumshoe
     *
     * adapted from SelectorProvider.loadProviderAsService jdk6
     */
    private static SelectorProvider getProviderAsService() {
        ServiceLoader<SelectorProvider> sl =
            ServiceLoader.load(SelectorProvider.class,
                               ClassLoader.getSystemClassLoader());
        Iterator<SelectorProvider> i = sl.iterator();
        for (;;) {
            try {
                if( ! i.hasNext()) return null;
                SelectorProvider candidate = i.next();
                if( ! (candidate instanceof IoTraceSelectorProvider)) { return candidate; }
            } catch (ServiceConfigurationError sce) {
                if (sce.getCause() instanceof SecurityException) {
                    // Ignore the security exception, try the next provider
                    continue;
                }
                throw sce;
            }
        }
    }

    ///// pass directly to delegate

    @Override
    public int hashCode() { return delegate.hashCode(); }
    @Override
    public boolean equals(Object obj) { return delegate.equals(obj); }
    @Override
    public Pipe openPipe() throws IOException { return delegate.openPipe(); }
    @Override
    public String toString() { return delegate.toString(); }

    ///// wrap value from delegate to call IoTrace

    private AbstractSelector wrap(AbstractSelector orig) {
        return orig instanceof Wrapper ? orig : new SelectorWrapper(orig);
    }
    private DatagramChannel wrap(DatagramChannel orig) {
        return(usePlainDatagramChannel || orig instanceof Wrapper) ? orig : new DatagramChannelWrapper(orig);
    }
    private ServerSocketChannel wrap(ServerSocketChannel orig) {
        return (usePlainServerSocketChannel || orig instanceof Wrapper) ? orig : new ServerSocketChannelWrapper(orig);
    }
    private SocketChannel wrap(SocketChannel orig) {
        return (usePlainSocketChannel || orig instanceof Wrapper) ? orig : new SocketChannelWrapper(orig);
    }

    private DatagramSocket wrap(DatagramSocket orig) {
        return orig;
    }

    private <T> T unwrap(T orig) {
        return (T) (orig instanceof Wrapper ? ((Wrapper)orig).unwrap() : orig);
    }
    /////

    @Override
    public AbstractSelector openSelector() throws IOException { return wrap(delegate.openSelector()); }

    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
        return wrap(delegate.openDatagramChannel());
    }

    @Override
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        return wrap(delegate.openDatagramChannel(family));
    }


    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return wrap(delegate.openServerSocketChannel());
    }

    @Override
    public SocketChannel openSocketChannel() throws IOException {
        return wrap(delegate.openSocketChannel());
    }

    @Override
    public Channel inheritedChannel() throws IOException {
        final Channel orig = delegate.inheritedChannel();
        if(orig instanceof Wrapper) { return orig; }
        if(orig instanceof FileChannel) { return orig; }
        if(orig instanceof SocketChannel) { return wrap((SocketChannel)orig); }
        if(orig instanceof DatagramChannel) { return wrap((DatagramChannel)orig); }
        // gumshoe I/O reporting not (yet?) supported for pipes, sctpsocket, others...
        return orig;
    }

    ///// wrappers invoke IoTrace in situations where original class does not

    private interface Wrapper<T> {
        T unwrap();
    }

    private void reflectCloseSelectableChannel(AbstractSelectableChannel target) throws IOException {
        try {
            closeChannelMethod.invoke(target);
        } catch (Exception e) {
            if(e instanceof IOException) { throw (IOException) e; }
            if(e.getCause() instanceof IOException) { throw (IOException) e.getCause(); }
            if(e instanceof RuntimeException) { throw (RuntimeException) e; }
            if(e.getCause() instanceof RuntimeException) { throw (RuntimeException) e.getCause(); }
            throw new RuntimeException("exception invoking implCloseSelectableChannel " + target, e);
        }
    }

    private void reflectCloseSelector(AbstractSelector target) throws IOException {
        try {
            closeSelectorMethod.invoke(target);
        } catch (Exception e) {
            if(e instanceof IOException) { throw (IOException) e; }
            if(e.getCause() instanceof IOException) { throw (IOException) e.getCause(); }
            if(e instanceof RuntimeException) { throw (RuntimeException) e; }
            if(e.getCause() instanceof RuntimeException) { throw (RuntimeException) e.getCause(); }
            throw new RuntimeException("exception invoking implCloseSelector " + target, e);
        }
    }

    private void reflectConfigureBlocking(AbstractSelectableChannel target, boolean arg) throws IOException {
        try {
            configureBlockingMethod.invoke(target, arg);
        } catch (Exception e) {
            if(e instanceof IOException) { throw (IOException) e; }
            if(e.getCause() instanceof IOException) { throw (IOException) e.getCause(); }
            if(e instanceof RuntimeException) { throw (RuntimeException) e; }
            if(e.getCause() instanceof RuntimeException) { throw (RuntimeException) e.getCause(); }
            throw new RuntimeException("exception invoking implConfigureBlocking " + target, e);
        }
    }

    private SelectionKey reflectRegister(AbstractSelector target, AbstractSelectableChannel ch, int ops, Object att) {
        try {
            return (SelectionKey) registerMethod.invoke(target, ch, ops, att);
        } catch (Exception e) {
            if(e instanceof RuntimeException) { throw (RuntimeException) e; }
            if(e.getCause() instanceof RuntimeException) { throw (RuntimeException) e.getCause(); }
            throw new RuntimeException("exception invoking register " + target, e);
        }

    }

    /** wrap DatagramChannel
     * most calls are just passed directly to delegate
     * others wrap return value
     * others invoke IoTrace
     */
    private class DatagramChannelWrapper extends DatagramChannel implements Wrapper<DatagramChannel> {
        private final DatagramChannel delegate;

        public DatagramChannelWrapper(DatagramChannel delegate) {
            super(IoTraceSelectorProvider.this);
            this.delegate = delegate;
        }

        public DatagramChannel unwrap() { return delegate; }

        ///// directly invoke delegate
        public int hashCode() { return delegate.hashCode(); }
        public SocketAddress getLocalAddress() throws IOException { return delegate.getLocalAddress(); }
        public boolean equals(Object obj) { return delegate.equals(obj); }
        public <T> T getOption(SocketOption<T> name) throws IOException { return delegate.getOption(name); }
        public Set<SocketOption<?>> supportedOptions() { return delegate.supportedOptions(); }
        public MembershipKey join(InetAddress group, NetworkInterface interf) throws IOException { return delegate.join(group, interf); }
        public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source) throws IOException { return delegate.join(group, interf, source); }
        public boolean isConnected() { return delegate.isConnected(); }
        public String toString() { return delegate.toString(); }
        public SocketAddress getRemoteAddress() throws IOException { return delegate.getRemoteAddress(); }

        ///// invoke delegate using reflection
        protected void implCloseSelectableChannel() throws IOException {
            reflectCloseSelectableChannel(delegate);
        }
        protected void implConfigureBlocking(boolean block) throws IOException {
            reflectConfigureBlocking(delegate, block);
        }

        ///// factory should handle correct impl
        public DatagramSocket socket() {
            return wrap(delegate.socket());
        }

        ///// wrap results
        public DatagramChannel bind(SocketAddress local) throws IOException {
            return wrap(delegate.bind(local));
        }
        public <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException {
            return wrap(delegate.setOption(name, value));
        }
        public DatagramChannel connect(SocketAddress remote) throws IOException {
            return wrap(delegate.connect(remote));
        }
        public DatagramChannel disconnect() throws IOException {
            return wrap(delegate.disconnect());
        }

        ///// invoke IoTrace
        public SocketAddress receive(ByteBuffer dst) throws IOException {
            final int positionBefore = dst==null ? 0 : dst.position();
            final Object context = IoTraceHandler.datagramReadBegin();
            final SocketAddress address = delegate.receive(dst);
            if(address!=null) {
                final int bytes = dst.position() - positionBefore;
                IoTraceHandler.datagramReadEnd(context, address, bytes);
            }
            return address;
        }

        public int send(ByteBuffer src, SocketAddress address) throws IOException {
            final Object context = IoTraceHandler.datagramWriteBegin();
            final int bytes = delegate.send(src, address);
            if(bytes>0) {
                IoTraceHandler.datagramWriteEnd(context, address, bytes);
            }
            return bytes;
        }

        public int read(ByteBuffer dst) throws IOException {
            final Object context = IoTraceHandler.datagramReadBegin();
            final int bytes = delegate.read(dst);
            if(bytes>0) {
                IoTraceHandler.datagramReadEnd(context, getRemoteAddress(), bytes);
            }
            return bytes;
        }

        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            final Object context = IoTraceHandler.datagramReadBegin();
            final long bytes = delegate.read(dsts, offset, length);
            if(bytes>0) {
                IoTraceHandler.datagramReadEnd(context, getRemoteAddress(), bytes);
            }
            return bytes;
        }

        public int write(ByteBuffer src) throws IOException {
            final Object context = IoTraceHandler.datagramWriteBegin();
            final int bytes = delegate.write(src);
            if(bytes>0) {
                IoTraceHandler.datagramWriteEnd(context, getRemoteAddress(), bytes);
            }
            return bytes;
        }

        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            final Object context = IoTraceHandler.datagramWriteBegin();
            final long bytes = delegate.write(srcs, offset, length);
            if(bytes>0) {
                IoTraceHandler.datagramWriteEnd(context, getRemoteAddress(), bytes);
            }
            return bytes;
        }
    }

    private class ServerSocketChannelWrapper extends ServerSocketChannel implements Wrapper<ServerSocketChannel> {
        private final ServerSocketChannel delegate;

        public ServerSocketChannelWrapper(ServerSocketChannel delegate) {
            super(IoTraceSelectorProvider.this);
            this.delegate = delegate;
        }

        public ServerSocketChannel unwrap() { return delegate; }

        ///// directly invoke delegate
        public int hashCode() { return delegate.hashCode(); }
        public SocketAddress getLocalAddress() throws IOException { return delegate.getLocalAddress(); }
        public boolean equals(Object obj) { return delegate.equals(obj); }
        public <T> T getOption(SocketOption<T> name) throws IOException { return delegate.getOption(name); }
        public Set<SocketOption<?>> supportedOptions() { return delegate.supportedOptions(); }
        public String toString() { return delegate.toString(); }

        ///// factory should handle correct impl
        public ServerSocket socket() {
            return delegate.socket();
        }

        ///// invoke delegate using reflection
        protected void implCloseSelectableChannel() throws IOException {
            reflectCloseSelectableChannel(delegate);
        }

        protected void implConfigureBlocking(boolean block) throws IOException {
            reflectConfigureBlocking(delegate, block);
        }

        ///// wrap results
        public ServerSocketChannel bind(SocketAddress local, int backlog)
                throws IOException {
            return delegate.bind(local, backlog);
        }


        public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
            return wrap(delegate.setOption(name, value));
        }

        public SocketChannel accept() throws IOException {
            return wrap(delegate.accept());
        }
    }

    private class SocketChannelWrapper extends SocketChannel implements Wrapper<SocketChannel> {
        private final SocketChannel delegate;

        public SocketChannelWrapper(SocketChannel delegate) {
            super(IoTraceSelectorProvider.this);
            this.delegate = delegate;
        }

        public SocketChannel unwrap() { return delegate; }

        ///// directly invoke delegate
        public int hashCode() { return delegate.hashCode(); }
        public SocketAddress getLocalAddress() throws IOException { return delegate.getLocalAddress(); }
        public boolean equals(Object obj) { return delegate.equals(obj); }
        public <T> T getOption(SocketOption<T> name) throws IOException { return delegate.getOption(name); }
        public Set<SocketOption<?>> supportedOptions() { return delegate.supportedOptions(); }
        public String toString() { return delegate.toString(); }
        public boolean isConnected() { return delegate.isConnected(); }
        public boolean isConnectionPending() { return delegate.isConnectionPending(); }
        public boolean connect(SocketAddress remote) throws IOException { return delegate.connect(remote); }
        public boolean finishConnect() throws IOException { return delegate.finishConnect(); }
        public SocketAddress getRemoteAddress() throws IOException { return delegate.getRemoteAddress(); }

        ///// invoke delegate using reflection
        protected void implCloseSelectableChannel() throws IOException {
            reflectCloseSelectableChannel(delegate);
        }

        protected void implConfigureBlocking(boolean block) throws IOException {
            reflectConfigureBlocking(delegate, block);
        }

        ///// wrap results
        public SocketChannel bind(SocketAddress local) throws IOException {
            return wrap(delegate.bind(local));
        }

        public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
            return wrap(delegate.setOption(name, value));
        }

        public SocketChannel shutdownInput() throws IOException {
            return wrap(delegate.shutdownInput());
        }

        public SocketChannel shutdownOutput() throws IOException {
            return wrap(delegate.shutdownOutput());
        }

        ///// factory should handle correct impl
        public Socket socket() {
            return delegate.socket();
        }

        ///// invoke IoTrace

        private boolean hasReadTrace() {
            return isBlocking(); // for SocketChannelImpl, others may vary
        }

        private boolean hasWriteTrace() {
            return true; // for SocketChannelImpl, others may vary
        }

        public int read(ByteBuffer dst) throws IOException {
            final Object context = hasReadTrace() ? null : IoTraceHandler.socketReadBegin();
            final int bytes = delegate.read(dst);
            if(bytes>0) {
                IoTraceHandler.socketReadEnd(context, getRemoteAddress(), bytes);
            }
            return bytes;
        }

        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            final Object context = hasReadTrace() ? null : IoTraceHandler.socketReadBegin();
            final long bytes = delegate.read(dsts, offset, length);
            if(bytes>0) {
                IoTraceHandler.socketReadEnd(context, getRemoteAddress(), bytes);
            }
            return bytes;
        }

        public int write(ByteBuffer src) throws IOException {
            final Object context = hasWriteTrace() ? null : IoTraceHandler.socketWriteBegin();
            final int bytes = delegate.write(src);
            if(bytes>0) {
                IoTraceHandler.socketWriteEnd(context, getRemoteAddress(), bytes);
            }
            return bytes;
        }

        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            final Object context = hasWriteTrace() ? null : IoTraceHandler.socketWriteBegin();
            final long bytes = delegate.write(srcs, offset, length);
            if(bytes>0) {
                IoTraceHandler.socketWriteEnd(context, getRemoteAddress(), bytes);
            }
            return bytes;
        }
    }

    private class SelectorWrapper extends AbstractSelector implements Wrapper<AbstractSelector> {
        private AbstractSelector delegate;

        public SelectorWrapper(AbstractSelector delegate) {
            super(IoTraceSelectorProvider.this);
            this.delegate = delegate;
        }

        public AbstractSelector unwrap() { return delegate; }

        public int hashCode() { return delegate.hashCode(); }
        public boolean equals(Object obj) { return delegate.equals(obj); }
        public Set<SelectionKey> keys() { return delegate.keys(); }
        public String toString() { return delegate.toString(); }
        public Set<SelectionKey> selectedKeys() { return delegate.selectedKeys(); }
        public int selectNow() throws IOException { return delegate.selectNow(); }
        public int select(long timeout) throws IOException { return delegate.select(timeout); }
        public int select() throws IOException { return delegate.select(); }
        public Selector wakeup() { return delegate.wakeup(); }

        ///// invoke delegate using reflection
        @Override
        protected void implCloseSelector() throws IOException {
            reflectCloseSelector(delegate);
        }

        @Override
        protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
            AbstractSelectableChannel unwrapped = IoTraceSelectorProvider.this.unwrap(ch);
            return reflectRegister(delegate, unwrapped, ops, att);
        }
    }
}
