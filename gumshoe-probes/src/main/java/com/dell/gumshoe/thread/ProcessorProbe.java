package com.dell.gumshoe.thread;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.socket.io.SocketIODetailAdder;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.stats.ValueReporter.StreamReporter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.Properties;

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
    public void initialize(Properties p) throws Exception {
        final boolean shutdownReportEnabled = isTrue(p, "gumshoe.cpu-usage.onshutdown", false);
        final Long periodicFrequency = getNumber(p, "gumshoe.cpu-usage.period");
        final boolean periodicReportEnabled = periodicFrequency!=null;
        final boolean reportEnabled = shutdownReportEnabled || periodicReportEnabled;

        // jmx enabled if explicit name, explicit property, or some reporting enabled
        final String mbeanName = getMBeanName(p, "gumshoe.cpu-usage.mbean.name", getClass());
        final boolean jmxEnabled = p.containsKey("gumshoe.cpu-usage.mbean.name")
                || isTrue(p, "gumshoe.cpu-usage.mbean", reportEnabled);

        final boolean enabled = isTrue(p, "gumshoe.cpu-usage.enabled", reportEnabled || jmxEnabled);
        if( ! enabled) { return; }

        StackFilter stackFilter = createStackFilter("gumshoe.cpu-usage.filter.", p);
        final PrintStream out = getOutput(p, "gumshoe.cpu-usage.output", System.out);

        final long sampleFrequency = getNumber(p, "gumshoe.cpu-usage.sample", 5000);
        final int threadPriority = (int) getNumber(p, "gumshoe.cpu-usage.priority", Thread.MIN_PRIORITY);

        final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        final boolean contentionSupported = mbean.isThreadContentionMonitoringSupported();
        final boolean enableContentionMonitoring = isTrue(p, "gumshoe.cpu-usage.use-wait-times", contentionSupported);

        initialize(sampleFrequency, threadPriority,
                shutdownReportEnabled, periodicFrequency, enableContentionMonitoring, jmxEnabled?mbeanName:null, stackFilter, out);
    }

    public void initialize(long sampleFrequency, int threadPriority, boolean shutdownReportEnabled, Long periodicFrequency,
            boolean enableContentionMonitoring, String mbeanName, StackFilter stackFilter, final PrintStream out) throws Exception {
        if(monitor!=null) throw new IllegalStateException("probe is already installed");

        monitor = new ThreadMonitor();
        monitor.setStackFilter(stackFilter);
        monitor.setDumpInterval(sampleFrequency);
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
