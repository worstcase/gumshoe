package com.dell.gumshoe.socket.unclosed;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.ValueReporter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

public class SocketCloseMBeanImpl implements SocketCloseMonitorMBean {
    private final Probe probe;

    public SocketCloseMBeanImpl(Probe probe) {
        this.probe = probe;
    }

    @Override public boolean isEnabled() { return probe.getUnclosedMonitor().isEnabled(); }
    @Override public void setEnabled(boolean enabled) { probe.getUnclosedMonitor().setEnabled(enabled); }

    @Override
    public String getReport(long age) {
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(rawOut);
        final ValueReporter<UnclosedStats>.StreamReporter streamer = probe.getUnclosedReporter().new StreamReporter(out);
        final Map<Stack,UnclosedStats> values = probe.getUnclosedMonitor().getStats(age);
        streamer.statsReported(values);
        return rawOut.toString();
    }

    @Override
    public void setReportingFrequency(long millis) {
        probe.getUnclosedReporter().scheduleReportTimer(probe.getTimer(), millis);
    }

    @Override
    public long getReportingFrequency() {
        return probe.getUnclosedReporter().getReportTimerFrequency();
    }

    @Override
    public void setClearClosedSocketsInterval(int numberOfSockets) {
        probe.getUnclosedMonitor().setClearClosedSocketsInterval(numberOfSockets);
    }

    @Override
    public int getClearClosedSocketsInterval() {
        return probe.getUnclosedMonitor().getClearClosedSocketsInterval();
    }

    @Override
    public void setShutdownReportEnabled(boolean enabled) {
        if(enabled) {
            probe.addShutdownHook(probe.getUnclosedReporter().getShutdownHook());
        } else {
            probe.removeShutdownHook(probe.getUnclosedReporter().getShutdownHook());
        }
    }

    @Override
    public boolean isShutdownReportEnabled() {
        return probe.isShutdownHookEnabled(probe.getUnclosedReporter().getShutdownHook());
    }
}
