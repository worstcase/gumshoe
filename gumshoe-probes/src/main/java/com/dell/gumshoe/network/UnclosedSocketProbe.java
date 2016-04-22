package com.dell.gumshoe.network;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.ValueReporter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class UnclosedSocketProbe extends Probe implements SocketCloseMonitorMBean {
    public static final String LABEL = "open-sockets";

    private SocketCloseMonitor monitor;
    private ValueReporter<UnclosedStats> reporter;

    public UnclosedSocketProbe(ProbeServices services) {
        super(services);
    }

    @Override
    public ValueReporter<UnclosedStats> getReporter() { return reporter; }

    /////

    @Override
    public void initialize(Properties p) throws Exception {
        final boolean shutdownReportEnabled = isTrue(p, "gumshoe.socket-unclosed.onshutdown", false);
        final Long periodicFrequency = getNumber(p, "gumshoe.socket-unclosed.period");
        final boolean periodicReportEnabled = periodicFrequency!=null;
        final boolean reportingEnabled = periodicReportEnabled || shutdownReportEnabled;

        // jmx enabled if explicit name, explicit property, or some reporting enabled
        final String mbeanName = getMBeanName(p, "gumshoe.socket-unclosed.mbean.name", getClass());
        final boolean jmxEnabled = p.containsKey("gumshoe.socket-unclosed.mbean.name")
                || isTrue(p, "gumshoe.socket-unclosed.mbean", reportingEnabled);

        // by default, enabled monitor only if reporting it or enabled mbean (but can override w/property)
        final boolean enabled = isTrue(p, "gumshoe.socket-unclosed.enabled", reportingEnabled || jmxEnabled);
        if( ! enabled) { return; }

        final long minAge = getNumber(p, "gumshoe.socket-unclosed.age", 30000);
        final int clearCount = (int) getNumber(p, "gumshoe.socket-unclosed.check-interval", 100);

        final StackFilter stackFilter = createStackFilter("gumshoe.socket-unclosed.filter.", p);

        final PrintStream out = getOutput(p, "gumshoe.socket-unclosed.output", System.out);
        initialize(shutdownReportEnabled, periodicFrequency, minAge, clearCount, stackFilter, jmxEnabled?mbeanName:null, out);
    }

    public void initialize(boolean shutdownReportEnabled, Long reportPeriod, final long minAge, int clearCount,
            StackFilter filter, String mbeanName, final PrintStream out) throws Exception {
        if(monitor!=null) throw new IllegalStateException("probe is already installed");

        monitor = new SocketCloseMonitor(minAge, filter);
        monitor.setClearClosedSocketsInterval(clearCount);

        reporter = new ValueReporter(LABEL, monitor);
        if(out!=null) {
            reporter.addStreamReporter(out);
        }

        if(shutdownReportEnabled) {
            addShutdownHook(reporter.getShutdownHook());
        }

        if(reportPeriod!=null) {
            reporter.scheduleReportTimer(getTimer(), reportPeriod);
        }

        if(mbeanName!=null) {
            installMBean(mbeanName, this, SocketCloseMonitorMBean.class);
        }

        monitor.initializeProbe();
    }

    /////

    @Override public boolean isEnabled() { return monitor.isEnabled(); }
    @Override public void setEnabled(boolean enabled) { monitor.setEnabled(enabled); }

    @Override
    public String getReport(long age) {
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(rawOut);
        final ValueReporter<UnclosedStats>.StreamReporter streamer = getReporter().new StreamReporter(out);
        final Map<Stack,UnclosedStats> values = monitor.getStats(age);
        streamer.statsReported(values);
        return rawOut.toString();
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
    public void setClearClosedSocketsInterval(int numberOfSockets) {
        monitor.setClearClosedSocketsInterval(numberOfSockets);
    }

    @Override
    public int getClearClosedSocketsInterval() {
        return monitor.getClearClosedSocketsInterval();
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
