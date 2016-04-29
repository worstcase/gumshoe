package com.dell.gumshoe.inspector.helper;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.network.DatagramIODetailAdder;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.stats.ValueReporter.Listener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import java.text.ParseException;

public class DatagramIOHelper extends IOHelper {
    private final JCheckBox accept = new JCheckBox("datagram IO", true);

    @Override
    public JComponent getSelectionComponent() { return accept; }

    @Override
    public boolean isSelected() { return accept.isSelected(); }

    @Override
    public StatisticAdder parse(String value) throws ParseException {
        return DatagramIODetailAdder.fromString(value);
    }

    @Override
    public void addListener(ProbeManager probe, Listener listener) {
        final ValueReporter<IODetailAdder> reporter = probe.getDatagramIOReporter();
        if(reporter!=null) {
            reporter.addListener(listener);
        }
    }

    @Override
    protected String getTargetName() {
        return "addresses";
    }
}
