package com.dell.gumshoe;

import com.dell.gumshoe.Probe.ProbeServices;
import com.dell.gumshoe.file.io.FileIOProbe;
import com.dell.gumshoe.io.IOAccumulator;
import com.dell.gumshoe.socket.io.SocketIOAccumulator;
import com.dell.gumshoe.socket.io.SocketIODetailAdder;
import com.dell.gumshoe.socket.io.SocketIOProbe;
import com.dell.gumshoe.socket.unclosed.UnclosedSocketProbe;
import com.dell.gumshoe.socket.unclosed.UnclosedStats;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.thread.CPUStats;
import com.dell.gumshoe.thread.ProcessorProbe;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Properties;

/** util to enable/disable monitoring tools
 *
 *  can wrap your main and run as java application:
 *      old cmdline: java ...opts... x.y.SomeClass args...
 *      new cmdline: java ...opts... com.dell.gumshoe.ProbeManager x.y.SomeClass args...
 *
 *  or make explicit from a java app:
 *      ProbeManager.initialize();
 */
public class ProbeManager {
    public static final String SOCKET_IO_LABEL = SocketIOProbe.LABEL;
    public static final String FILE_IO_LABEL = FileIOProbe.LABEL;
    public static final String UNCLOSED_SOCKET_LABEL = UnclosedSocketProbe.LABEL;
    public static final String CPU_USAGE_LABEL = ProcessorProbe.LABEL;

    public static ProbeManager MAIN_INSTANCE;

    public static void main(String... args) throws Throwable {
        final String[] newArgs = new String[args.length-1];
        System.arraycopy(args, 1, newArgs, 0, args.length-1);

        MAIN_INSTANCE = new ProbeManager();
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

    private ProbeServices sharedServices;
    private SocketIOProbe socketIOProbe;
    private FileIOProbe fileIOProbe;
    private UnclosedSocketProbe unclosedSocketProbe;
    private ProcessorProbe cpuProbe;

    public void setOutput(String key, PrintStream value) {
        sharedServices.putNamedOutput(key, value);
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

    protected void initialize(InputStream rawIn, Properties overrides) throws Exception {
        final Properties fileProperties = new Properties();
        fileProperties.load(rawIn instanceof BufferedInputStream ? rawIn : new BufferedInputStream(rawIn));

        final Properties combinedProperties = new Properties(fileProperties);
        combinedProperties.putAll(overrides);
        initialize(combinedProperties);
    }

    public void initialize(Properties p) throws Exception {
        sharedServices = new ProbeServices();
        sharedServices.installShutdownHook();

        socketIOProbe = new SocketIOProbe(sharedServices);
        fileIOProbe = new FileIOProbe(sharedServices);
        unclosedSocketProbe = new UnclosedSocketProbe(sharedServices);
        cpuProbe = new ProcessorProbe(sharedServices);

        socketIOProbe.initialize(p);
        fileIOProbe.initialize(p);
        unclosedSocketProbe.initialize(p);
        cpuProbe.initialize(p);
    }

    public void destroy() { }

    /////

    public ValueReporter<UnclosedStats> getUnclosedReporter() {
        return unclosedSocketProbe.getReporter();
    }

    public ValueReporter<IODetailAdder> getSocketIOReporter() {
        return socketIOProbe.getReporter();
    }

    public ValueReporter<IODetailAdder> getFileIOReporter() {
        return fileIOProbe.getReporter();
    }

    public ValueReporter<CPUStats> getCPUReporter() {
        return cpuProbe.getReporter();
    }


    /////

    public static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() { @Override public void write(int b) { } };
    public static final PrintStream NULL_PRINT_STREAM = new NullPrintStream();

    protected static class NullPrintStream extends PrintStream {
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
}
