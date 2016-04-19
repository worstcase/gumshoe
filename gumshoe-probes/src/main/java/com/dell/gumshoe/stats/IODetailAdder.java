package com.dell.gumshoe.stats;

import com.dell.gumshoe.io.IOEvent;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class IODetailAdder implements StatisticAdder<IOEvent> {
    public static final MessageFormat FORMAT =
            new MessageFormat("{0,number,#} read ops {1,number,#} bytes in {2,number,#} ms, {3,number,#} write ops {4,number,#} bytes in {5,number,#} ms: [{6}]");

    public final Set<String> targets = new ConcurrentSkipListSet<>();
    public final AtomicLong readBytes = new AtomicLong();
    public final AtomicLong readTime = new AtomicLong();
    public final AtomicInteger readCount = new AtomicInteger();
    public final AtomicLong writeBytes = new AtomicLong();
    public final AtomicLong writeTime = new AtomicLong();
    public final AtomicInteger writeCount = new AtomicInteger();

    @Override
    public void add(StatisticAdder<IOEvent> that) {
        if(that instanceof IODetailAdder) {
            add((IODetailAdder)that);
        }
    }

    public void add(IODetailAdder that) {
        targets.addAll(that.targets);
        readBytes.addAndGet(that.readBytes.get());
        readTime.addAndGet(that.readTime.get());
        readCount.addAndGet(that.readCount.get());
        writeBytes.addAndGet(that.writeBytes.get());
        writeTime.addAndGet(that.writeTime.get());
        writeCount.addAndGet(that.writeCount.get());

    }
    @Override
    public void add(IOEvent event) {
        targets.add(event.getTarget());
        if(event.isRead()) {
            readBytes.addAndGet(event.getBytes());
            readTime.addAndGet(event.getElapsed());
            readCount.incrementAndGet();
        } else {
            writeBytes.addAndGet(event.getBytes());
            writeTime.addAndGet(event.getElapsed());
            writeCount.incrementAndGet();
        }
    }

    @Override
    public String toString() {
        final Object[] arg = {
                readCount.get(), readBytes.get(), readTime.get(),
                writeCount.get(), writeBytes.get(), writeTime.get(),
                targets.toString().replaceAll("[\\[\\]]", "")
        };

        synchronized(FORMAT) {
            return FORMAT.format(arg);
        }
    }

    protected void setFromString(String line) throws ParseException {
        final Object[] fields;
        synchronized(FORMAT) {
            fields = FORMAT.parse(line);
        }

        this.readCount.set(((Number)fields[0]).intValue());
        this.readBytes.set(((Number)fields[1]).longValue());
        this.readTime.set(((Number)fields[2]).longValue());
        this.writeCount.set(((Number)fields[3]).intValue());
        this.writeBytes.set(((Number)fields[4]).longValue());
        this.writeTime.set(((Number)fields[5]).longValue());

        final String addressCSV = (String)fields[6];
        this.targets.clear();
        this.targets.addAll(Arrays.asList(addressCSV.replaceAll(" ", "").split(",")));
    }

}