package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.stats.StatisticAccumulator;

import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class IODetailAccumulator implements StatisticAccumulator<IODetail> {
    public final Set<String> addresses = new ConcurrentSkipListSet<>();
    public final AtomicLong readBytes = new AtomicLong();
    public final AtomicLong readTime = new AtomicLong();
    public final AtomicInteger readCount = new AtomicInteger();
    public final AtomicLong writeBytes = new AtomicLong();
    public final AtomicLong writeTime = new AtomicLong();
    public final AtomicInteger writeCount = new AtomicInteger();

    @Override
    public void add(StatisticAccumulator<IODetail>  that) {
        if(that instanceof IODetailAccumulator) {
            add((IODetailAccumulator)that);
        } else {
            add(that.get());
        }
    }

    public void add(IODetailAccumulator that) {
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
    public IODetailAccumulator newInstance() {
        return new IODetailAccumulator();
    }

    @Override
    public String toString() {
        return String.format("%d r %d bytes in %d ms, %d w %d bytes in %d ms",
                readCount.get(), readBytes.get(), readTime.get(),
                writeCount.get(), writeBytes.get(), writeTime.get());
    }

    public static IODetailAccumulator fromString(String line) throws ParseException {
        final Object[] args;
        synchronized(IODetail.FORMAT) {
            args = IODetail.FORMAT.parse(line);
        }
        final IODetailAccumulator out = new IODetailAccumulator();
        final String addressField = (String)args[6];
        final String addresses = addressField.replaceAll("[\\[\\]]", "");
        out.add(new IODetail(addresses, (Long)args[1], (Long)args[2], ((Number)args[0]).intValue(),(Long) args[4], (Long)args[5], ((Number)args[3]).intValue()));
        return out;
    }
}