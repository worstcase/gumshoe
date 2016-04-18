package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.socket.SocketMatcher;
import com.dell.gumshoe.socket.SocketMatcherSeries;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.ValueReporter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class SocketIOProbe extends Probe implements SocketIOMBean {
    public static final String LABEL = "socket-io";

    private SocketIOMonitor monitor;
    private SocketIOAccumulator accumulator;
    private ValueReporter<SocketIODetailAdder> reporter;

    public SocketIOProbe(ProbeServices services) {
        super(services);
    }

    public SocketIOAccumulator getAccumulator() {
        return accumulator;
    }

    @Override
    public ValueReporter<SocketIODetailAdder> getReporter() {
        return reporter;
    }

    @Override
    public void initialize(Properties p) throws Exception {
        final boolean shutdownReportEnabled = isTrue(p, "gumshoe.socket-io.onshutdown", false);
        final Long periodicFrequency = getNumber(p, "gumshoe.socket-io.period");
        final boolean periodicReportEnabled = periodicFrequency!=null;
        final boolean reportEnabled = shutdownReportEnabled || periodicReportEnabled;

        // jmx enabled if explicit name, explicit property, or some reporting enabled
        final String mbeanName = getMBeanName(p, "gumshoe.socket-io.mbean.name", getClass());
        final boolean jmxEnabled = p.containsKey("gumshoe.socket-io.mbean.name")
                || isTrue(p, "gumshoe.socket-io.mbean", reportEnabled);

        final boolean enabled = isTrue(p, "gumshoe.socket-io.enabled", reportEnabled || jmxEnabled);
        if( ! enabled) { return; }

        StackFilter stackFilter = createStackFilter("gumshoe.socket-io.filter.", p);

        final SocketMatcher[] acceptList = parseSocketMatchers(p.getProperty("gumshoe.socket-io.include"));
        final SocketMatcher[] rejectList = parseSocketMatchers(p.getProperty("gumshoe.socket-io.exclude", "127.0.0.1/32:*"));
        final SocketMatcherSeries socketFilter = new SocketMatcherSeries(acceptList, rejectList);

        final PrintStream out = getOutput(p, "gumshoe.socket-io.output", System.out);

        initialize(shutdownReportEnabled, periodicFrequency, jmxEnabled?mbeanName:null, socketFilter, stackFilter, out);
    }

    public void initialize(boolean shutdownReportEnabled, Long periodicFrequency, String mbeanName,
            SocketMatcher socketFilter, StackFilter stackFilter, final PrintStream out) throws Exception {
        if(monitor!=null) throw new IllegalStateException("probe is already installed");

        monitor = new SocketIOMonitor(socketFilter);

        accumulator = new SocketIOAccumulator(stackFilter);
        monitor.addListener(accumulator);

        reporter = new ValueReporter(LABEL, accumulator);
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
            installMBean(mbeanName, this, SocketIOMBean.class);
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

    @Override
    public String getReport() {
        final ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(rawOut);
        final ValueReporter<SocketIODetailAdder>.StreamReporter streamer = getReporter().new StreamReporter(out);
        final Map<Stack,SocketIODetailAdder> values = getAccumulator().getStats();
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
}
