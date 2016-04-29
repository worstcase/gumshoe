package com.dell.gumshoe.inspector.helper;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.file.FileIODetailAdder;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.stats.ValueReporter.Listener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import java.text.ParseException;

public class FileIOHelper extends IOHelper {
    private final JCheckBox acceptFileIO = new JCheckBox("file IO", true);

    @Override
    public JComponent getSelectionComponent() { return acceptFileIO; }

    @Override
    public boolean isSelected() { return acceptFileIO.isSelected(); }

    @Override
    public StatisticAdder parse(String value) throws ParseException {
        return FileIODetailAdder.fromString(value);
    }

    @Override
    public void addListener(ProbeManager probe, Listener listener) {
        final ValueReporter<IODetailAdder> reporter = probe.getFileIOReporter();
        if(reporter!=null) {
            reporter.addListener(listener);
        }
    }

    @Override
    protected String getTargetName() {
        return "files";
    }
}
