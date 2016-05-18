package com.dell.gumshoe.thread;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.util.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;

public class ProcessorProbe extends Probe implements CpuUsageMBean {
    public static final String LABEL = "cpu-usage";

    private ThreadMonitor monitor;
    private ValueReporter<CPUStats> reporter;

    public ProcessorProbe(ProbeServices services) {
        super(services);
    }

    @Override
    public ValueReporter<CPUStats> getReporter() { return reporter; }

    /////

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
        if( ! enabled) { return; }

        StackFilter stackFilter = createStackFilter(cfg);
        final PrintStream out = getOutput(cfg);

        final long sampleFrequency = cfg.getNumber("sample", 5000);
        final long jitter = cfg.getNumber("jitter", 1000);
        final int threadPriority = (int) cfg.getNumber("priority", Thread.MIN_PRIORITY);

        final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        final boolean contentionSupported = mbean.isThreadContentionMonitoringSupported();
        final boolean enableContentionMonitoring = cfg.isTrue("use-wait-times", contentionSupported);

        initialize(sampleFrequency, jitter, threadPriority,
                shutdownReportEnabled, periodicFrequency, enableContentionMonitoring, jmxEnabled?mbeanName:null, stackFilter, out);
    }

    public void initialize(long sampleFrequency, long jitter, int threadPriority, boolean shutdownReportEnabled, Long periodicFrequency,
            boolean enableContentionMonitoring, String mbeanName, StackFilter stackFilter, final PrintStream out) throws Exception {
        if(monitor!=null) throw new IllegalStateException("probe is already installed");

        monitor = new ThreadMonitor();
        monitor.setStackFilter(stackFilter);
        monitor.setDumpInterval(sampleFrequency);
        monitor.setJitter(jitter);
        monitor.setThreadPriority(threadPriority);
        monitor.setContentionMonitoringEnabled(enableContentionMonitoring);

        reporter = new ValueReporter(LABEL, monitor);
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
            installMBean(mbeanName, this, CpuUsageMBean.class);
        }

        monitor.initializeProbe();
    }

    ///// jmx

    public long getJitter() {
        return monitor.getJitter();
    }

    public void setJitter(long jitter) {
        monitor.setJitter(jitter);
    }

    @Override
    public long getDumpInterval() {
        return monitor.getDumpInterval();
    }

    @Override
    public void setDumpInterval(long dumpInterval) {
        monitor.setDumpInterval(dumpInterval);
    }

    @Override
    public int getThreadPriority() {
        return monitor.getThreadPriority();
    }

    @Override
    public void setThreadPriority(int threadPriority) {
        monitor.setThreadPriority(threadPriority);
    }

    @Override
    public long getEffectiveInterval() {
        return monitor.getEffectiveInterval();
    }

    @Override
    public void setEnabled(boolean enabled) {
        monitor.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return monitor.isEnabled();
    }

    @Override
    public String getReport() {
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(rawOut);
        final ValueReporter<CPUStats>.StreamReporter streamer = getReporter().new StreamReporter(out);
        final Map<Stack,CPUStats> values = monitor.getStats();
        streamer.statsReported(values);
        return rawOut.toString();
    }


    @Override
    public void reset() {
        monitor.reset();
    }

    @Override
    public long getAverageDumpTime() {
        return monitor.getAverageDumpTime();
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
}
