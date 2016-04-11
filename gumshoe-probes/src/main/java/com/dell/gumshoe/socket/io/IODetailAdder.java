package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.stats.StatisticAdder;

import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class IODetailAdder implements StatisticAdder<IODetail> {
    public final Set<String> addresses = new ConcurrentSkipListSet<>();
    public final AtomicLong readBytes = new AtomicLong();
    public final AtomicLong readTime = new AtomicLong();
    public final AtomicInteger readCount = new AtomicInteger();
    public final AtomicLong writeBytes = new AtomicLong();
    public final AtomicLong writeTime = new AtomicLong();
    public final AtomicInteger writeCount = new AtomicInteger();

    @Override
    public void add(StatisticAdder<IODetail>  that) {
        if(that instanceof IODetailAdder) {
            add((IODetailAdder)that);
        } else {
            add(that.get());
        }
    }

    public void add(IODetailAdder that) {
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
    public String toString() {
        return String.format("%d r %d bytes in %d ms, %d w %d bytes in %d ms",
                readCount.get(), readBytes.get(), readTime.get(),
                writeCount.get(), writeBytes.get(), writeTime.get());
    }
}