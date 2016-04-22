package com.dell.gumshoe.io;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.file.FileIOAccumulator;
import com.dell.gumshoe.network.SocketIOAccumulator;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.ValueReporter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public abstract class IOProbe extends Probe implements IOMBean {
    private IOMonitor monitor;
    private IOAccumulator accumulator;
    private ValueReporter<IODetailAdder> reporter;

    public abstract String getLabel();
    protected abstract IOMonitor createMonitor(Properties p) throws Exception;
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

    protected String getPropertyName(String partial) {
        return "gumshoe." + getLabel() + "." + partial;
    }

    @Override
    public void initialize(Properties p) throws Exception {
        final boolean shutdownReportEnabled = isTrue(p, getPropertyName("onshutdown"), false);
        final Long periodicFrequency = getNumber(p, getPropertyName("period"));
        final boolean periodicReportEnabled = periodicFrequency!=null;
        final boolean reportEnabled = shutdownReportEnabled || periodicReportEnabled;

        // jmx enabled if explicit name, explicit property, or some reporting enabled
        final String mbeanName = getMBeanName(p, getPropertyName("mbean.name"), getClass());
        final boolean jmxEnabled = p.containsKey(getPropertyName("mbean.name"))
                || isTrue(p, getPropertyName("mbean"), reportEnabled);

        final boolean enabled = isTrue(p, getPropertyName("enabled"), reportEnabled || jmxEnabled);
        if( ! enabled) { return; }

        final StackFilter stackFilter = createStackFilter(getPropertyName("filter."), p);
        final PrintStream out = getOutput(p, getPropertyName("output"), System.out);

        final IOMonitor monitor = createMonitor(p);
        initialize(monitor, shutdownReportEnabled, periodicFrequency, jmxEnabled?mbeanName:null, stackFilter, out);
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
}
