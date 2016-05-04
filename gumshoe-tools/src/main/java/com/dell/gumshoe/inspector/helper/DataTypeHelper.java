package com.dell.gumshoe.inspector.helper;

import static com.dell.gumshoe.util.Swing.flow;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.inspector.graph.StackFrameNode;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter.Listener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class DataTypeHelper {
    public abstract String getToolTipText(StackFrameNode boxNode, StackFrameNode parentNode);
    public abstract String getStatDetails(StatisticAdder boxNode, StatisticAdder parentNode);
    public abstract StatisticAdder parse(String value) throws Exception;
    public abstract String getSummary(Map<Stack, StatisticAdder> data);
    public abstract JComponent getOptionEditor();
    public abstract JComponent getSelectionComponent();
    public abstract boolean isSelected();
    public abstract void addListener(ProbeManager probe, Listener listener);
    public abstract long getStatValue(StatisticAdder composite);

    protected JPanel getDisclaimer() {
        return flow(new JLabel("** = these stats are not additive; widths from different stacks are not comparable"));
    }

    protected String pct(Number num, Number num2, Number div, Number div2) {
        return pct(num.longValue()+num2.longValue(), div.longValue()+div2.longValue());
    }

    protected String pct(Number num, Number div) {
        if(div.longValue()==0) { return ""; }
        return " " + (100*num.longValue()/div.longValue()) + "%";
    }

    protected long div(Number num, Number den) {
        return den.longValue()==0 ? num.longValue() : Math.round(num.floatValue()/den.floatValue());
    }

    protected String getFrames(Collection<StackTraceElement> frames) {
        final StringBuilder out = new StringBuilder();
        for(StackTraceElement frame : frames) {
            out.append("\n ").append(frame);
        }
        return out.toString();
    }

    public String getDetailText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final StringBuilder msg = new StringBuilder();
        msg.append(String.format("Frame: %s\n\nContext:\n", boxNode.getFrame()));
        boxNode.appendContext(msg);
        msg.append("\n");

        msg.append(getStatDetails(boxNode.getDetail(), parentNode.getDetail()));

        final Set<StackTraceElement> callingFrames = boxNode.getCallingFrames();
        final Set<StackTraceElement> calledFrames = boxNode.getCalledFrames();
        msg.append(String.format("Calls %d methods: %s\n\nCalled by %d methods: %s",
                calledFrames.size(), getFrames(calledFrames),
                callingFrames.size(), getFrames(callingFrames) ));

        return msg.toString();
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

    public static String getStatInfo(StatisticAdder target, StatisticAdder parent) {
        return forType(target.getType()).getStatDetails(target, parent);
    }
}
