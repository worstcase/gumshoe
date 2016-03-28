package com.dell.gumshoe;

import com.dell.gumshoe.socket.SocketCloseMonitor;
import com.dell.gumshoe.socket.SocketCloseMonitorMBean;
import com.dell.gumshoe.socket.SocketIOAccumulator;
import com.dell.gumshoe.socket.SocketIOMonitor;
import com.dell.gumshoe.socket.SocketIOStackReporter;
import com.dell.gumshoe.socket.SocketIOStackReporter.StreamReporter;
import com.dell.gumshoe.socket.SocketMatcher;
import com.dell.gumshoe.socket.SocketMatcherSeries;
import com.dell.gumshoe.socket.SubnetAddress;
import com.dell.gumshoe.stack.Filter;
import com.dell.gumshoe.stack.Filter.Builder;
import com.dell.gumshoe.stack.StackFilter;

import javax.management.ObjectName;
import javax.management.StandardMBean;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/** util to enable/disable monitoring tools
 *
 *  can wrap your main and run as java application:
 *      old cmdline: java ...opts... x.y.SomeClass args...
 *      new cmdline: java ...opts... com.dell.gumshoe.Probe x.y.SomeClass args...
 *
 *  or make explicit from a java app:
 *      Probe.initialize();
 */
public class Probe {
    public static Probe MAIN_INSTANCE;

    public static void main(String... args) throws Throwable {
        final String[] newArgs = new String[args.length-1];
        System.arraycopy(args, 1, newArgs, 0, args.length-1);

        MAIN_INSTANCE = new Probe();
        MAIN_INSTANCE.initialize();

        final Class mainClass = Class.forName(args[0]);
        final Method mainMethod = mainClass.getDeclaredMethod("main", args.getClass());
        try {
            mainMethod.invoke(mainClass, new Object[] { newArgs });
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /////

    private Timer timer;
    private SocketCloseMonitor closeMonitor;
    private SocketIOMonitor ioMonitor;
    private SocketIOAccumulator ioAccumulator;
    private SocketIOStackReporter ioReporter;
    private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();
    private Map<String,PrintStream> namedOutput = new HashMap<>();

    public Probe() {
        final Thread shutdownThread = new Thread() {
            @Override
            public void run() {
                for(Runnable task : shutdownHooks) {
                    try {
                        task.run();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        shutdownThread.setName("gumshoe-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    public void setOutput(String key, PrintStream value) {
        namedOutput.put(key, value);
    }

    private synchronized Timer getTimer() {
        if(timer==null) {
            timer = new Timer(true);
        }
        return timer;
    }

    ///// util methods

    private static boolean isTrue(Properties p, String key, boolean defaultValue) {
        return "true".equalsIgnoreCase(p.getProperty(key, Boolean.toString(defaultValue)));
    }

    private static long getNumber(Properties p, String key, long defaultValue) {
        return Long.parseLong(p.getProperty(key, Long.toString(defaultValue)));
    }

    private static Long getNumber(Properties p, String key) {
        final String stringValue = p.getProperty(key);
        return stringValue==null ? null : Long.parseLong(stringValue);
    }

    private static String[] getList(Properties p, String key) {
        final String stringValue = p.getProperty(key);
        if(stringValue==null || stringValue.isEmpty()) { return new String[0]; }
        final String[] out = stringValue.split(",");
        for(int i=0;i<out.length;i++) {
            out[i] = out[i].trim();
        }
        return out;
    }
    private static SocketMatcher[] parseSocketMatchers(String csv) throws ParseException {
        if(csv==null || csv.trim().equals("")) {
            return new SocketMatcher[0];
        }

        final String[] addressDescriptions = csv.split(",");
        final int len = addressDescriptions.length;
        final SocketMatcher[] matchers = new SocketMatcher[len];
        for(int i=0;i<len;i++) {
            matchers[i] = new SubnetAddress(addressDescriptions[i].trim());
        }
        return matchers;
    }

    private static String getMBeanName(Properties p, String key, Class clazz) {
        String mbeanName = p.getProperty(key);
        if(mbeanName==null) {
            final String packageName = clazz.getPackage().getName();
            final String className = clazz.getSimpleName();
            mbeanName = packageName + ":type=" + className;
        }
        return mbeanName;
    }

    private static void installMBean(String name, Object impl, Class type) throws Exception {
        final ObjectName nameObj = new ObjectName(name);
        StandardMBean standardMBean = new StandardMBean(impl, type);
        ManagementFactory.getPlatformMBeanServer().registerMBean(standardMBean, nameObj);
    }

    private synchronized PrintStream getOutput(Properties p, String key, PrintStream defaultOut) throws Exception {
        final String propValue = p.getProperty(key);
        if(propValue==null) { return defaultOut; }
        if("none".equals(propValue)) { return NULL_PRINT_STREAM; }
        if("stdout".equals(propValue) || "-".equals(propValue)) { return System.out; }
        if(propValue.startsWith("file:")) {
            return new PrintStream(new URI(propValue).getPath());
        }
        if(propValue.startsWith("name:")) {
            final String name = propValue.split(":")[1];
            final PrintStream explicitValue = namedOutput.get(name);
            return explicitValue!=null ? explicitValue : new DefineLaterPrintStream(name);
        }
        throw new IllegalArgumentException("unrecognized output " + key + " = " + propValue);
    }

    ///// main use case: initialize()

    public void initialize() throws Exception {
        final String fileName = System.getProperty("gumshoe.config", "gumshoe.properties");

        // first try in classpath
        final InputStream configResource = getClass().getClassLoader().getResourceAsStream(fileName);
        if(configResource!=null) {
            try {
                initialize(configResource, System.getProperties());
                return;
            } finally {
                configResource.close();
            }
        }

        // then try as file pathname
        final File configFile = new File(fileName);
        if(configFile.exists()) {
            final InputStream fileStream = new FileInputStream(fileName);
            try {
                initialize(fileStream, System.getProperties());
                return;
            } finally {
                fileStream.close();
            }
        }

        // not found, use just properties
        initialize(System.getProperties());
    }

    private void initialize(InputStream rawIn, Properties overrides) throws Exception {
        final Properties fileProperties = new Properties();
        fileProperties.load(rawIn instanceof BufferedInputStream ? rawIn : new BufferedInputStream(rawIn));

        final Properties combinedProperties = new Properties(fileProperties);
        combinedProperties.putAll(overrides);
        initialize(combinedProperties);
    }

    public void initialize(Properties p) throws Exception {
        initializeSocketCloseMonitor(p);
        initializeIOMonitor(p);
    }

    public void destroy() { }


    ///// unclosed socket monitor

    public SocketCloseMonitor initializeSocketCloseMonitor(Properties p) throws Exception {
        final boolean shutdownReportEnabled = isTrue(p, "gumshoe.socket-unclosed.onshutdown", false);
        final Long periodicFrequency = getNumber(p, "gumshoe.socket-unclosed.period");
        final boolean periodicReportEnabled = periodicFrequency!=null;
        final boolean reportingEnabled = periodicReportEnabled || shutdownReportEnabled;

        // by default, jmx enabled if reporting it (but can override w/property)
        final boolean jmxEnabled = isTrue(p, "gumshoe.socket-unclosed.mbean", reportingEnabled);
        final String mbeanName = jmxEnabled ?
                getMBeanName(p, "gumshoe.socket-unclosed.mbean.name", SocketCloseMonitor.class) : null;

        // by default, enabled monitor only if reporting it or enabled mbean (but can override w/property)
        final boolean enabled = isTrue(p, "gumshoe.socket-unclosed.enabled", reportingEnabled || jmxEnabled);
        if( ! enabled) { return null; }

        final long minAge = getNumber(p, "gumshoe.socket-unclosed.age", 30000);
        final int clearCount = (int) getNumber(p, "gumshoe.socket-unclosed.check-interval", 100);

        final PrintStream out = getOutput(p, "gumshoe.socket-unclosed.output", System.out);
        return initializeSocketCloseMonitor(shutdownReportEnabled, periodicFrequency, minAge, clearCount, mbeanName, out);
    }

    public SocketCloseMonitor initializeSocketCloseMonitor(boolean shutdownReportEnabled, Long reportPeriod, final long minAge, int clearCount, String mbeanName, final PrintStream out) throws Exception {
        if(closeMonitor!=null) throw new IllegalStateException("monitor is already installed");

        closeMonitor = new SocketCloseMonitor();
        closeMonitor.setClearClosedSocketsInterval(clearCount);

        closeMonitor.initializeProbe();

        if(mbeanName!=null) {
            installMBean(mbeanName, closeMonitor, SocketCloseMonitorMBean.class);
        }

        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                out.println(closeMonitor.getReport(minAge));
            }
        };
        if(shutdownReportEnabled) {
            addShutdownHook(task);
        }
        if(reportPeriod!=null) {
            getTimer().scheduleAtFixedRate(task, reportPeriod, reportPeriod);
        }
        return closeMonitor;
    }

    ///// socket io monitor

    public SocketIOMonitor initializeIOMonitor(Properties p) throws Exception {
            final boolean shutdownReportEnabled = isTrue(p, "gumshoe.socket-io.onshutdown", false);
            final Long periodicFrequency = getNumber(p, "gumshoe.socket-io.period");
            final boolean periodicReportEnabled = periodicFrequency!=null;
            final boolean reportEnabled = shutdownReportEnabled || periodicReportEnabled;
            final boolean jmxEnabled = isTrue(p, "gumshoe.socket-io.mbean", reportEnabled);
            final boolean enabled = isTrue(p, "gumshoe.socket-io.enabled", reportEnabled || jmxEnabled);
            if( ! enabled) { return null; }

            StackFilter stackFilter = initializeSocketFilter("gumshoe.socket-io.filter.", p);

            final SocketMatcher[] acceptList = parseSocketMatchers(p.getProperty("gumshoe.socket-io.include"));
            final SocketMatcher[] rejectList = parseSocketMatchers(p.getProperty("gumshoe.socket-io.exclude", "127.0.0.1/32:*"));
            final SocketMatcherSeries socketFilter = new SocketMatcherSeries(acceptList, rejectList);

            final PrintStream out = getOutput(p, "gumshoe.socket-io.output", System.out);

            return initializeIOMonitor(shutdownReportEnabled, periodicFrequency, socketFilter, stackFilter, out);
    }

    /** create stack filter for socket I/O
     *
     *  property names shown begin with prefix ("gumshoe.socket.filter." or "gumshoe.fileio.filter.")
     *
     *  these properties will add filters as described in order:
     *      none                raw stacks used, no other filters apply
     *      exclude-jdk         drop stack frames from packages: sun, sunw, java, javax, and com.dell.gumshoe
     *      include             comma-separated list of package or fully-qualified class names
     *                          if set, only frames matching these packages or classes will be included
     *                          if not set, will include all frames not specifically excluded
     *      exclude             comma-separated list of package or fully-qualified class names to exclude
     *                          if set, frames matching these packages or classes will be excluded,
     *                          even if they also match an include package or class
     *      top                 number of frames from the top of the stack to retain
     *      bottom              number of frames from the bottom of the stack to retain
     *                          if both top and bottom are set, frames from both ends are retained
     *                          if neither is set, all frames are retained
     *                          if only one is set, the value of the other is assumed to be zero
     *      allow-empty-stack   applies only if the above filters leave no stack frames in the result
     *                          if set to false, the raw stack is used if filters would have removed all frame
     *                          otherwise (the default) the empty stack becomes kind of a catch-all "other" category
     */
    private StackFilter initializeSocketFilter(String prefix, Properties p) {
        if(isTrue(p, prefix + "none", false)) { return Filter.NONE; }

        final Builder builder = Filter.builder();
        if( ! isTrue(p, prefix + "allow-empty-stack", true)) { builder.withOriginalIfBlank(); }
        if(isTrue(p, prefix + "exclude-jdk", true)) { builder.withExcludePlatform(); }
        for(String matching : getList(p, prefix + "include")) {
            builder.withOnlyClasses(matching);
        }
        for(String matching : getList(p, prefix + "exclude")) {
            builder.withExcludeClasses(matching);
        }
        final int topCount = (int)getNumber(p, prefix + "top", 0);
        final int bottomCount = (int)getNumber(p, prefix + "bottom", 0);
        if(topCount>0 || bottomCount>0) {
            builder.withEndsOnly(topCount, bottomCount);
        }
        return builder.build();
    }

    public SocketIOMonitor initializeIOMonitor(boolean shutdownReportEnabled, Long periodicFrequency, SocketMatcher socketFilter, StackFilter stackFilter, final PrintStream out) throws Exception {
        if(ioMonitor!=null) throw new IllegalStateException("monitor is already installed");

        ioAccumulator = new SocketIOAccumulator(stackFilter);

        ioMonitor = new SocketIOMonitor(socketFilter);
        ioMonitor.addListener(ioAccumulator);

        ioReporter = new SocketIOStackReporter(ioAccumulator);
        if(shutdownReportEnabled) {
            addShutdownHook(ioReporter);
        }
        if(periodicFrequency!=null) {
            getTimer().scheduleAtFixedRate(ioReporter, periodicFrequency, periodicFrequency);
        }
        if(out!=null) {
            StreamReporter r = new StreamReporter(out);
            ioReporter.addListener(r);
        }

        ioMonitor.initializeProbe();
        return ioMonitor;
    }

    public SocketIOMonitor getIOMonitor() {
        return ioMonitor;
    }

    public SocketIOAccumulator getIOAccumulator() {
        return ioAccumulator;
    }

    public SocketIOStackReporter getIOReporter() {
        return ioReporter;
    }

    /////

    private void addShutdownHook(Runnable task) {
        shutdownHooks.add(task);
    }

    /////

    private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() { @Override public void write(int b) { } };
    private static final PrintStream NULL_PRINT_STREAM = new NullPrintStream();

    private static class NullPrintStream extends PrintStream {
        public NullPrintStream() { super(NULL_OUTPUT_STREAM); }
        @Override public void flush() { }
        @Override public void close() { }
        @Override public void print(boolean b){ }

        @Override public void print(char c) { }
        @Override public void print(int i) { }
        @Override public void print(long l) { }
        @Override public void print(float f) { }
        @Override public void print(double d) { }
        @Override public void print(char[] s) { }
        @Override public void print(String s) { }
        @Override public void print(Object obj) { }
        @Override public void println() { }
        @Override public void println(boolean x) { }
        @Override public void println(char x) { }
        @Override public void println(int x) { }
        @Override public void println(long x) { }
        @Override public void println(float x) { }
        @Override public void println(double x) { }
        @Override public void println(char[] x) { }
        @Override public void println(String x) { }
        @Override public void println(Object x) { }
        @Override public PrintStream printf(String format, Object... args) { return this; }
        @Override public PrintStream printf(Locale l, String format, Object... args) { return this; }
        @Override public PrintStream format(String format, Object... args) { return this; }
        @Override public PrintStream format(Locale l, String format, Object... args) { return this; }
        @Override public PrintStream append(CharSequence csq) { return this; }
        @Override public PrintStream append(CharSequence csq, int start, int end) { return this; }
        @Override public PrintStream append(char c) { return this; }    }

    /** probe may be started right as JVM starts,
     * but we may need to let the monitored program start and reach some hook
     * to programmatically define more interesting outputs.  so this
     *
     */
    private class DefineLaterPrintStream extends PrintStream {
        private String key;
        private volatile PrintStream delegate;

        public DefineLaterPrintStream(String key) {
            super(NULL_OUTPUT_STREAM);
            this.key = key;
        }

        private PrintStream getDelegate() {
            PrintStream localCopy = delegate;
            if(localCopy==null) {
                localCopy = setDelegate();
            }
            return localCopy==null ? NULL_PRINT_STREAM : localCopy;
        }

        private synchronized PrintStream setDelegate() {
            delegate = namedOutput.get(key);
            return delegate;
        }

        private synchronized void clearDelegate() {
            delegate = null;
        }

        /** when closed, delegate is closed but stream can be reopened
         *  next output will re-fetch from namedOutput map,
         *  so change map value before closing this stream
         *  to switch to a new output
         */
        @Override
        public void close() {
            getDelegate().close();
            clearDelegate();
        }

        @Override
        public void write(byte[] b) throws IOException { getDelegate().write(b); }
        @Override
        public void flush() { getDelegate().flush(); }
        @Override
        public boolean checkError() { return getDelegate().checkError(); }
        @Override
        public void write(int b) { getDelegate().write(b); }
        @Override
        public void write(byte[] buf, int off, int len) { getDelegate().write(buf, off, len); }
        @Override
        public void print(boolean b) { getDelegate().print(b); }
        @Override
        public void print(char c) { getDelegate().print(c); }
        @Override
        public void print(int i) { getDelegate().print(i); }
        @Override
        public void print(long l) { getDelegate().print(l); }
        @Override
        public void print(float f) { getDelegate().print(f); }
        @Override
        public void print(double d) { getDelegate().print(d); }
        @Override
        public void print(char[] s) { getDelegate().print(s); }
        @Override
        public void print(String s) { getDelegate().print(s); }
        @Override
        public void print(Object obj) { getDelegate().print(obj); }
        @Override
        public void println() { getDelegate().println(); }
        @Override
        public void println(boolean x) { getDelegate().println(x); }
        @Override
        public void println(char x) { getDelegate().println(x); }
        @Override
        public void println(int x) { getDelegate().println(x); }
        @Override
        public void println(long x) { getDelegate().println(x); }
        @Override
        public void println(float x) { getDelegate().println(x); }
        @Override
        public void println(double x) { getDelegate().println(x); }
        @Override
        public void println(char[] x) { getDelegate().println(x); }
        @Override
        public void println(String x) { getDelegate().println(x); }
        @Override
        public void println(Object x) { getDelegate().println(x); }
        @Override
        public PrintStream printf(String format, Object... args) { return getDelegate().printf(format, args); }
        @Override
        public PrintStream printf(Locale l, String format, Object... args) { return getDelegate().printf(l, format, args); }
        @Override
        public PrintStream format(String format, Object... args) { return getDelegate().format(format, args); }
        @Override
        public PrintStream format(Locale l, String format, Object... args) { return getDelegate().format(l, format, args); }
        @Override
        public PrintStream append(CharSequence csq) { return getDelegate().append(csq); }
        @Override
        public PrintStream append(CharSequence csq, int start, int end) { return getDelegate().append(csq, start, end); }
        @Override
        public PrintStream append(char c) { return getDelegate().append(c); }


    }
}
