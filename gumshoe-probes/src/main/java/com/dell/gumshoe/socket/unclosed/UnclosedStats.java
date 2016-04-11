package com.dell.gumshoe.socket.unclosed;

import com.dell.gumshoe.socket.unclosed.SocketCloseMonitor.SocketImplDecorator;
import com.dell.gumshoe.stats.StatisticAccumulator;

public class UnclosedStats implements StatisticAccumulator<UnclosedStats> {
    private int count;
    private long earliestStart = Long.MAX_VALUE;

    public void add(SocketImplDecorator item) {
        this.count++;
        this.earliestStart = Math.min(this.earliestStart, item.openTime.getTime());
    }

    @Override
    public void add(StatisticAccumulator<UnclosedStats> value) {
        add(this.get());
    }

    @Override
    public void add(UnclosedStats value) {
        this.count += value.count;
        this.earliestStart = Math.min(this.earliestStart, value.earliestStart);
    }

    @Override
    public UnclosedStats get() {
        return this;
    }

    @Override
    public StatisticAccumulator<UnclosedStats> newInstance() {
        return new UnclosedStats();
    }

    @Override
    public String toString() {
        return String.format("%d sockets, first opened %d seconds ago", count, (System.currentTimeMillis() - earliestStart)/1000);
    }
}