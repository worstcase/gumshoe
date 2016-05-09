package com.dell.gumshoe;

import static com.dell.gumshoe.util.Output.configure;

import com.dell.gumshoe.Probe.ProbeServices;
import com.dell.gumshoe.file.FileIOProbe;
import com.dell.gumshoe.network.DatagramIOProbe;
import com.dell.gumshoe.network.SocketIOProbe;
import com.dell.gumshoe.network.UnclosedSocketProbe;
import com.dell.gumshoe.network.UnclosedStats;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.thread.CPUStats;
import com.dell.gumshoe.thread.ProcessorProbe;
import com.dell.gumshoe.util.Configuration;

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
    public static final String DATAGRAM_IO_LABEL = DatagramIOProbe.LABEL;

    private static ProbeManager INSTANCE = new ProbeManager();
    public static ProbeManager getInstance() { return INSTANCE; }

    private ProbeManager() { }

    /////

    private ProbeServices sharedServices;
    private SocketIOProbe socketIOProbe;
    private DatagramIOProbe datagramIOProbe;
    private FileIOProbe fileIOProbe;
    private UnclosedSocketProbe unclosedSocketProbe;
    private ProcessorProbe cpuProbe;

    public void setOutput(String key, PrintStream value) {
        sharedServices.putNamedOutput(key, value);
    }

    ///// main use case: initialize()

    public void initialize() throws Exception {
        initialize(new Configuration());
    }

    public void initialize(Properties p) throws Exception {
        final Configuration cfg = new Configuration(p);
        initialize(cfg);

        configure(cfg); // configure logging/output
    }

    private void initialize(Configuration p) throws Exception {
        sharedServices = new ProbeServices();
        sharedServices.installShutdownHook();

        socketIOProbe = new SocketIOProbe(sharedServices);
        datagramIOProbe = new DatagramIOProbe(sharedServices);
        fileIOProbe = new FileIOProbe(sharedServices);
        unclosedSocketProbe = new UnclosedSocketProbe(sharedServices);
        cpuProbe = new ProcessorProbe(sharedServices);

        socketIOProbe.initialize(p.withPrefix("socket-io"));
        datagramIOProbe.initialize(p.withPrefix("datagram-io"));
        fileIOProbe.initialize(p.withPrefix("file-io"));
        unclosedSocketProbe.initialize(p.withPrefix("socket-unclosed"));
        cpuProbe.initialize(p.withPrefix("cpu-usage"));
    }

    public boolean isUsingIoTrace() {
        return socketIOProbe.isAttached() || fileIOProbe.isAttached();
    }

    /////

    public ValueReporter<UnclosedStats> getUnclosedReporter() {
        return unclosedSocketProbe.getReporter();
    }

    public ValueReporter<IODetailAdder> getSocketIOReporter() {
        return socketIOProbe.getReporter();
    }

    public ValueReporter<IODetailAdder> getDatagramIOReporter() {
        return datagramIOProbe.getReporter();
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
