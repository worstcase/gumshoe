package com.dell.gumshoe.tools.stats;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter.Listener;
import com.dell.gumshoe.tools.graph.StackFrameNode;

import javax.swing.JComponent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class DataTypeHelper {
    public abstract String getToolTipText(StackFrameNode boxNode, StackFrameNode parentNode);
    public abstract String getDetailText(StackFrameNode boxNode, StackFrameNode parentNode);
    public abstract StatisticAdder parse(String value) throws Exception;
    public abstract String getSummary(Map<Stack, StatisticAdder> data);
    public abstract JComponent getOptionEditor();
    public abstract JComponent getSelectionComponent();
    public abstract boolean isSelected();
    public abstract void addListener(ProbeManager probe, Listener listener);
    public abstract long getStatValue(StatisticAdder composite);


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

    private static final Map<String,DataTypeHelper> impl = new LinkedHashMap<>();

    private static Map<String,DataTypeHelper> getImpl() {
        if(impl.isEmpty()) {
            impl.put(ProbeManager.CPU_USAGE_LABEL, new CPUUsageHelper());
            impl.put(ProbeManager.SOCKET_IO_LABEL, new SocketIOHelper());
            impl.put(ProbeManager.DATAGRAM_IO_LABEL, new DatagramIOHelper());
            impl.put(ProbeManager.FILE_IO_LABEL, new FileIOHelper());
            impl.put(ProbeManager.UNCLOSED_SOCKET_LABEL, new UnclosedSocketHelper());
        }
        return impl;
    }

    public static Collection<String> getTypes() { return getImpl().keySet(); }

    public static DataTypeHelper forType(String type) {
        return getImpl().get(type);
    }

    public static long getValue(StatisticAdder composite) {
        return forType(composite.getType()).getStatValue(composite);
    }

    public interface TypeDisplayOptions { }
}
