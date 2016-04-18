package com.dell.gumshoe.util;

import com.dell.gumshoe.ProbeManager;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;

/** probe may be started right as JVM starts,
 * but we may need to let the monitored program start and reach some hook
 * to programmatically define more interesting outputs.  so this
 *
 */
public class DefineLaterPrintStream extends PrintStream {
    private final Map<String,PrintStream> namedOutput;
    private String key;
    private volatile PrintStream delegate;

    public DefineLaterPrintStream(String key, Map<String,PrintStream> namedOutput) {
        super(ProbeManager.NULL_OUTPUT_STREAM);
        this.namedOutput = namedOutput;
        this.key = key;
    }

    private PrintStream getDelegate() {
        PrintStream localCopy = delegate;
        if(localCopy==null) {
            localCopy = setDelegate();
        }
        return localCopy==null ? ProbeManager.NULL_PRINT_STREAM : localCopy;
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

    @Override public void write(byte[] b) throws IOException { getDelegate().write(b); }
    @Override public void flush() { getDelegate().flush(); }
    @Override public boolean checkError() { return getDelegate().checkError(); }
    @Override public void write(int b) { getDelegate().write(b); }
    @Override public void write(byte[] buf, int off, int len) { getDelegate().write(buf, off, len); }
    @Override public void print(boolean b) { getDelegate().print(b); }
    @Override public void print(char c) { getDelegate().print(c); }
    @Override public void print(int i) { getDelegate().print(i); }
    @Override public void print(long l) { getDelegate().print(l); }
    @Override public void print(float f) { getDelegate().print(f); }
    @Override public void print(double d) { getDelegate().print(d); }
    @Override public void print(char[] s) { getDelegate().print(s); }
    @Override public void print(String s) { getDelegate().print(s); }
    @Override public void print(Object obj) { getDelegate().print(obj); }
    @Override public void println() { getDelegate().println(); }
    @Override public void println(boolean x) { getDelegate().println(x); }
    @Override public void println(char x) { getDelegate().println(x); }
    @Override public void println(int x) { getDelegate().println(x); }
    @Override public void println(long x) { getDelegate().println(x); }
    @Override public void println(float x) { getDelegate().println(x); }
    @Override public void println(double x) { getDelegate().println(x); }
    @Override public void println(char[] x) { getDelegate().println(x); }
    @Override public void println(String x) { getDelegate().println(x); }
    @Override public void println(Object x) { getDelegate().println(x); }
    @Override public PrintStream printf(String format, Object... args) { return getDelegate().printf(format, args); }
    @Override public PrintStream printf(Locale l, String format, Object... args) { return getDelegate().printf(l, format, args); }
    @Override public PrintStream format(String format, Object... args) { return getDelegate().format(format, args); }
    @Override public PrintStream format(Locale l, String format, Object... args) { return getDelegate().format(l, format, args); }
    @Override public PrintStream append(CharSequence csq) { return getDelegate().append(csq); }
    @Override public PrintStream append(CharSequence csq, int start, int end) { return getDelegate().append(csq, start, end); }
    @Override public PrintStream append(char c) { return getDelegate().append(c); }
}