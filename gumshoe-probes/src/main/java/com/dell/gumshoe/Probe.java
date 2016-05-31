package com.dell.gumshoe;

import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stack.StandardFilter;
import com.dell.gumshoe.stack.StandardFilter.Builder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.util.Configuration;
import com.dell.gumshoe.util.DefineLaterPrintStream;

import javax.management.ObjectName;
import javax.management.StandardMBean;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
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
public abstract class Probe {
    private final ProbeServices services;

    public Probe(ProbeServices services) {
        this.services = services;
    }

    ///// life cycle

    public abstract void initialize(Configuration cfg) throws Exception;
    public void destroy() throws Exception { }
    public abstract ValueReporter getReporter();

    ///// initialization util methods

    protected static boolean isTrue(Properties p, String key, boolean defaultValue) {
        return "true".equalsIgnoreCase(p.getProperty(key, Boolean.toString(defaultValue)));
    }

    protected static long getNumber(Properties p, String key, long defaultValue) {
        return Long.parseLong(p.getProperty(key, Long.toString(defaultValue)));
    }

    protected static Long getNumber(Properties p, String key) {
        final String stringValue = p.getProperty(key);
        return stringValue==null ? null : Long.parseLong(stringValue);
    }

    protected static String[] getList(Properties p, String key) {
        final String stringValue = p.getProperty(key);
        if(stringValue==null || stringValue.isEmpty()) { return new String[0]; }
        final String[] out = stringValue.split(",");
        for(int i=0;i<out.length;i++) {
            out[i] = out[i].trim();
        }
        return out;
    }

    protected static String getMBeanName(Configuration p) {
        return p.getProperty("mean.name");
    }

    protected static String getMBeanName(Configuration p, Class clazz) {
        final String packageName = clazz.getPackage().getName();
        final String className = clazz.getSimpleName();
        return p.getProperty("mbean.name", packageName + ":type=" + className);
    }

    protected static String getMBeanName(Properties p, String key, Class clazz) {
        String mbeanName = p.getProperty(key);
        if(mbeanName==null) {
            final String packageName = clazz.getPackage().getName();
            final String className = clazz.getSimpleName();
            mbeanName = packageName + ":type=" + className;
        }
        return mbeanName;
    }

    protected static void installMBean(String name, Object impl, Class type) throws Exception {
        final ObjectName nameObj = new ObjectName(name);
        StandardMBean standardMBean = new StandardMBean(impl, type);
        ManagementFactory.getPlatformMBeanServer().registerMBean(standardMBean, nameObj);
    }

    protected PrintStream getOutput(Properties p, String key, PrintStream defaultOut) throws Exception {
        return System.out;
    }

    protected PrintStream getOutput(Configuration p) throws Exception {
        final String propValue = p.getProperty("output");
        if(propValue==null || "stdout".equals(propValue) || "-".equals(propValue)) { return System.out; }
        if("none".equals(propValue)) { return ProbeManager.NULL_PRINT_STREAM; }
        if(propValue.startsWith("file:")) {
            return new PrintStream(new URI(propValue).getPath());
        }
        if(propValue.startsWith("name:")) {
            final String name = propValue.split(":")[1];
            final PrintStream explicitValue = services.getNamedOutput(name);
            return explicitValue!=null ? explicitValue : new DefineLaterPrintStream(name, services.getNamedOutput());
        }
        throw new IllegalArgumentException("unrecognized output: " + propValue);
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
    protected static StackFilter createStackFilter(Configuration p) {
        final Configuration cfg = new Configuration(p, "filter");

        if(cfg.isTrue("none", false)) { return StackFilter.NONE; }

        final Builder builder = StandardFilter.builder();
        if( ! cfg.isTrue("allow-empty-stack", true)) { builder.withOriginalIfBlank(); }
        if(cfg.isTrue("exclude-jdk", true)) { builder.withExcludePlatform(); }
        for(String matching : cfg.getList("include")) {
            builder.withOnlyClasses(matching);
        }
        for(String matching : cfg.getList("exclude")) {
            builder.withExcludeClasses(matching);
        }
        final int topCount = (int)cfg.getNumber("top", 0);
        final int bottomCount = (int)cfg.getNumber("bottom", 0);
        if(topCount>0 || bottomCount>0) {
            builder.withEndsOnly(topCount, bottomCount);
        }

        final Long recursionThreshold = cfg.getNumber("recursion.threshold");
        if(recursionThreshold!=null) {
            final int recursionLevel = (int)cfg.getNumber("recursion.depth", 1);
            builder.withRecursionFilter(recursionLevel, recursionThreshold.intValue());
        }

        final String simplifyLevel = cfg.getProperty("simplify");
        if(simplifyLevel!=null) {
            builder.withSimpleFrames(simplifyLevel);
        }
        return builder.build();
    }

    ///// shared services

    protected Timer getTimer() {
        return services.getTimer();
    }

    public void addShutdownHook(Runnable task) {
        final List<Runnable> shutdownHooks = services.getShutdownHooks();
        if(shutdownHooks.contains(task)) { throw new IllegalStateException("shutdown hook already enabled: " + task); }
        shutdownHooks.add(task);
    }

    public boolean removeShutdownHook(Runnable task) {
        final List<Runnable> shutdownHooks = services.getShutdownHooks();
        if( ! shutdownHooks.contains(task)) { throw new IllegalStateException("shutdown hook was not enabled: " + task); }
        return shutdownHooks.remove(task);
    }

    public boolean isShutdownHookEnabled(Runnable task) {
        final List<Runnable> shutdownHooks = services.getShutdownHooks();
        return shutdownHooks.contains(task);
    }

    public static class ProbeServices {
        private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();
        private final Map<String,PrintStream> namedOutput = new HashMap<>();
        private final Timer timer = new Timer(true);

        public Timer getTimer() { return timer; }
        public List<Runnable> getShutdownHooks() { return shutdownHooks; }
        public Map<String, PrintStream> getNamedOutput() { return namedOutput; }

        public PrintStream getNamedOutput(String name) {
            return namedOutput.get(name);
        }

        public void putNamedOutput(String key, PrintStream value) {
            namedOutput.put(key, value);
        }

        public void installShutdownHook() {
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
    }
}
