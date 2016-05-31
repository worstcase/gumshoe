package com.dell.gumshoe.io;

import static com.dell.gumshoe.util.Output.debug;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.util.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

public abstract class IOProbe extends Probe implements IOMBean {
    private IOMonitor monitor;
    private IOAccumulator accumulator;
    private ValueReporter<IODetailAdder> reporter;

    public abstract String getLabel();
    protected abstract IOMonitor createMonitor(Configuration cfg) throws Exception;
    protected abstract IOAccumulator createAccumulator(StackFilter stackFilter);

    public IOProbe(ProbeServices services) {
        super(services);
    }

    public IOAccumulator getAccumulator() {
        return accumulator;
    }

    @Override
    public ValueReporter<IODetailAdder> getReporter() {
        return reporter;
    }

    @Override
    public void initialize(Configuration cfg) throws Exception {
        final boolean shutdownReportEnabled = cfg.isTrue("onshutdown", false);
        final Long periodicFrequency = cfg.getNumber("period");
        final boolean periodicReportEnabled = periodicFrequency!=null;
        final boolean reportEnabled = shutdownReportEnabled || periodicReportEnabled;

        // jmx enabled if explicit name, explicit property, or some reporting enabled
        final boolean jmxEnabled =
                getMBeanName(cfg)!=null || cfg.isTrue("mbean", reportEnabled);
        final String mbeanName = jmxEnabled ? getMBeanName(cfg, getClass()) : null;

        final boolean enabled = cfg.isTrue("enabled", reportEnabled || jmxEnabled);
        if( ! enabled  && ! jmxEnabled) { return; }

        debug("installing probe", getClass().getSimpleName());
        final StackFilter stackFilter = createStackFilter(cfg);
        final PrintStream out = getOutput(cfg);

        final IOMonitor monitor = createMonitor(cfg);
        initialize(monitor, shutdownReportEnabled, periodicFrequency, jmxEnabled?mbeanName:null, stackFilter, out);
        monitor.setEnabled(enabled);
    }

    public void initialize(IOMonitor m, boolean shutdownReportEnabled, Long periodicFrequency, String mbeanName,
            StackFilter stackFilter, final PrintStream out) throws Exception {
        if(this.monitor!=null) throw new IllegalStateException("probe is already installed");

        monitor = m;

        accumulator = createAccumulator(stackFilter);
        monitor.addListener(accumulator);
        reporter = new ValueReporter(getLabel(), accumulator);
        if(shutdownReportEnabled) {
            addShutdownHook(reporter.getShutdownHook());
        }
        if(periodicFrequency!=null) {
            reporter.scheduleReportTimer(getTimer(), periodicFrequency);
        }
        if(out!=null) {
            reporter.addStreamReporter(out);
        }

        if(mbeanName!=null) {
            installMBean(mbeanName, this, IOMBean.class);
        }

        monitor.initializeProbe();
    }

    ///// mbean

    @Override
    public void setEnabled(boolean enabled) {
        monitor.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return monitor.isEnabled();
    }

    public boolean isAttached() {
        return monitor!=null;
    }

    @Override
    public String getReport() {
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(rawOut);
        final ValueReporter<IODetailAdder>.StreamReporter streamer = getReporter().new StreamReporter(out);
        final Map<Stack,IODetailAdder> values = getAccumulator().getStats();
        streamer.statsReported(values);
        return rawOut.toString();
    }

    @Override
    public void reset() {
        getAccumulator().reset();
    }

    @Override
    public void setReportingFrequency(long millis) {
        getReporter().scheduleReportTimer(getTimer(), millis);
    }

    @Override
    public long getReportingFrequency() {
        return getReporter().getReportTimerFrequency();
    }

    @Override
    public void setShutdownReportEnabled(boolean enabled) {
        if(enabled) {
            addShutdownHook(getReporter().getShutdownHook());
        } else {
            removeShutdownHook(getReporter().getShutdownHook());
        }
    }

    @Override
    public boolean isShutdownReportEnabled() {
        return isShutdownHookEnabled(getReporter().getShutdownHook());
    }

    public void setHandlerThreadCount(int count) {
        monitor.setThreadCount(count);
    }
    public int getHandlerThreadCount() {
        return monitor.getThreadCount();
    }

    public void setHandlerPriority(int priority) {
        monitor.setThreadPriority(priority);
    }
    public int getHandlerPriority() {
        return monitor.getThreadPriority();
    }

    public int getEventQueueSize() {
        return monitor.getEventQueueSize();
    }

    public String getQueueStats() {
        return monitor.getQueueStats();
    }

    public void resetQueueCounters() {
        monitor.resetQueueCounters();
    }

    public void setQueueStatisticsEnabled(boolean enabled) {
        monitor.setQueueStatisticsEnabled(enabled);
    }

    public boolean isQueueStatisticsEnabled() {
        return monitor.isQueueStatisticsEnabled();
    }

}
