package com.dell.gumshoe.socket;

import com.dell.gumshoe.socket.SocketIOMonitor.Event;
import com.dell.gumshoe.socket.SocketIOMonitor.Listener;
import com.dell.gumshoe.socket.SocketIOMonitor.RW;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public interface SocketIOListener extends Listener {

    @Override
    public void socketIOHasCompleted(Event event);

    /////

    public static class IODetail {
        private static String convertAddress(InetAddress addr, int port) {
            final byte[] ip = addr.getAddress();
            return String.format("%d.%d.%d.%d:%d", 255&ip[0], 255&ip[1], 255&ip[2], 255&ip[3], port);
        }

        private final String address;
        private final long readBytes, readTime, writeBytes, writeTime;
        private final int readCount, writeCount;

        public IODetail(Event e) {
            this(convertAddress(e.getAddress(), e.getPort()), e.getReadBytes(), e.getReadElapsed(), e.getRw()==RW.READ?1:0, e.getWriteBytes(), e.getWriteElapsed(), e.getRw()==RW.WRITE?1:0);
        }

        public IODetail(String address, long readBytes, long readTime, long writeBytes, long writeTime) {
            this(address, readBytes, readTime, 1, writeBytes, writeTime, 1);
        }

        public IODetail(String address, long readBytes, long readTime, int readCount, long writeBytes, long writeTime, int writeCount) {
            this.address = address;
            this.readBytes = readBytes;
            this.readTime = readTime;
            this.writeBytes = writeBytes;
            this.writeTime = writeTime;
            this.readCount = readCount;
            this.writeCount = writeCount;
        }

        @Override
        public String toString() {
            return String.format("%d read ops %d bytes in %d ms, %d write ops %d bytes in %d ms: %s",
                    readCount, readBytes, readTime, writeCount, writeBytes, writeTime, address);
        }
    }

    public static interface ValueType<V> {
        public void add(V value);
        public V get();
        public ValueType<V> newInstance();
    }

    public static class DetailAccumulator implements ValueType<IODetail> {
        public final Set<String> addresses = new ConcurrentSkipListSet<>();
        public final AtomicLong readBytes = new AtomicLong();
        public final AtomicLong readTime = new AtomicLong();
        public final AtomicInteger readCount = new AtomicInteger();
        public final AtomicLong writeBytes = new AtomicLong();
        public final AtomicLong writeTime = new AtomicLong();
        public final AtomicInteger writeCount = new AtomicInteger();

        public void add(DetailAccumulator that) {
            addresses.addAll(that.addresses);
            readBytes.addAndGet(that.readBytes.get());
            readTime.addAndGet(that.readTime.get());
            readCount.addAndGet(that.readCount.get());
            writeBytes.addAndGet(that.writeBytes.get());
            writeTime.addAndGet(that.writeTime.get());
            writeCount.addAndGet(that.writeCount.get());

        }
        @Override
        public void add(IODetail value) {
            addresses.add(value.address);
            readBytes.addAndGet(value.readBytes);
            readTime.addAndGet(value.readTime);
            readCount.addAndGet(value.readCount);
            writeBytes.addAndGet(value.writeBytes);
            writeTime.addAndGet(value.writeTime);
            writeCount.addAndGet(value.writeCount);
        }

        @Override
        public IODetail get() {
            return new IODetail(addresses.toString(), readBytes.get(), readTime.get(), readCount.get(), writeBytes.get(), writeTime.get(), writeCount.get());
        }

        @Override
        public DetailAccumulator newInstance() {
            return new DetailAccumulator();
        }

        @Override
        public String toString() {
            return String.format("%d r %d bytes in %d ms, %d w %d bytes in %d ms",
                    readCount.get(), readBytes.get(), readTime.get(),
                    writeCount.get(), writeBytes.get(), writeTime.get());
        }
    }
}
