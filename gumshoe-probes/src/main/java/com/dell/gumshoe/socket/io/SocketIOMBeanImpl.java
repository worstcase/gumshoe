package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.socket.IO.IOStats;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.stats.ValueReporter.StreamReporter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

public class SocketIOMBeanImpl implements SocketIOMBean {
    private final Probe probe;

    public SocketIOMBeanImpl(Probe probe) {
        this.probe = probe;
    }

    @Override
    public void setEnabled(boolean enabled) {
        probe.getIOMonitor().setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return probe.getIOMonitor().isEnabled();
    }

    @Override
    public String getReport() {
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(rawOut);
        final ValueReporter<SocketIODetailAdder>.StreamReporter streamer = probe.getIOReporter().new StreamReporter(out);
        final Map<Stack,SocketIODetailAdder> values = probe.getIOAccumulator().getStats();
        streamer.statsReported(values);
        return rawOut.toString();
    }

    @Override
    public void reset() {
        probe.getIOAccumulator().reset();
    }

    @Override
    public void setReportingFrequency(long millis) {
        probe.getIOReporter().scheduleReportTimer(probe.getTimer(), millis);
    }

    @Override
    public long getReportingFrequency() {
        return probe.getIOReporter().getReportTimerFrequency();
    }

    @Override
    public void setShutdownReportEnabled(boolean enabled) {
        if(enabled) {
            probe.addShutdownHook(probe.getIOReporter().getShutdownHook());
        } else {
            probe.removeShutdownHook(probe.getIOReporter().getShutdownHook());
        }
    }

    @Override
    public boolean isShutdownReportEnabled() {
        return probe.isShutdownHookEnabled(probe.getIOReporter().getShutdownHook());
    }

}
