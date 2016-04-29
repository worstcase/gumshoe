package com.dell.gumshoe.inspector.helper;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.network.SocketIODetailAdder;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.stats.ValueReporter.Listener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import java.text.ParseException;

public class SocketIOHelper extends IOHelper {

    private final JCheckBox acceptSocketIO = new JCheckBox("socket IO", true);

    @Override
    public JComponent getSelectionComponent() { return acceptSocketIO; }

    @Override
    public boolean isSelected() { return acceptSocketIO.isSelected(); }

    @Override
    public StatisticAdder parse(String value) throws ParseException {
        return SocketIODetailAdder.fromString(value);
    }

    @Override
    public void addListener(ProbeManager probe, Listener listener) {
        final ValueReporter<IODetailAdder> reporter = probe.getSocketIOReporter();
        if(reporter!=null) {
            reporter.addListener(listener);
        }
    }

    @Override
    protected String getTargetName() {
        return "addresses";
    }
}
