package com.dell.gumshoe.tools.stats;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.tools.graph.StackFrameNode;

import javax.swing.JComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class DataTypeHelper {
    public abstract String getToolTipText(StackFrameNode boxNode, StackFrameNode parentNode);
    public abstract String getDetailText(StackFrameNode boxNode, StackFrameNode parentNode);
    public abstract StatisticAdder parse(String value) throws Exception;
    public abstract String getSummary(Map<Stack, StatisticAdder> data);
    public abstract JComponent getOptionEditor();
    protected abstract long getStatValue(StatisticAdder composite);

    protected String getPercent(Number num, Number num2, Number div, Number div2) {
        return getPercent(num.longValue()+num2.longValue(), div.longValue()+div2.longValue());
    }

    protected String getPercent(Number num, Number div) {
        if(div.longValue()==0) { return ""; }
        return " " + (100*num.longValue()/div.longValue()) + "%";
    }

    protected String getFrames(Set<StackTraceElement> frames) {
        final StringBuilder out = new StringBuilder();
        for(StackTraceElement frame : frames) {
            out.append("\n ").append(frame);
        }
        return out.toString();
    }

    /////

    private static final Map<String,DataTypeHelper> impl = new HashMap<>();

    private static Map<String,DataTypeHelper> getImpl() {
        if(impl.isEmpty()) {
            impl.put(Probe.SOCKET_IO_LABEL, new SocketIOHelper());
            impl.put(Probe.UNCLOSED_SOCKET_LABEL, new UnclosedSocketHelper());
        }
        return impl;
    }

    public static DataTypeHelper forType(String type) {
        return getImpl().get(type);
    }

    public static long getValue(StatisticAdder composite) {
        return forType(composite.getType()).getStatValue(composite);
    }

    public interface TypeDisplayOptions { }
}
