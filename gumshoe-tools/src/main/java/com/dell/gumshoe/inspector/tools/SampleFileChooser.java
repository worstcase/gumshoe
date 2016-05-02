package com.dell.gumshoe.inspector.tools;

import com.dell.gumshoe.inspector.FileDataParser;
import com.dell.gumshoe.inspector.SampleSource;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class SampleFileChooser extends JFileChooser implements SampleSource {
    private static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");

    private final List<SampleSelectionListener> listeners = new CopyOnWriteArrayList<>();

    private FileDataParser parser;

    public SampleFileChooser() {
        setApproveButtonText("Parse");
    }

    public void approveSelection() {
        closeParser();
        openParser(getSelectedFile());
        notifyContentsChanged();
        super.approveSelection();
    }

    private void notifyError(String message) {
        System.out.println("need popup here: " + message);
    }

    private void closeParser() {
        if(parser!=null) {
            try { parser.close(); }
            catch(Exception ignore) { }
        }

        parser = null;
    }

    private void openParser(File file) {
        try {
            if( ! file.isFile()) {
                notifyError("Not a file: " + file);
            } else if( ! file.canRead()) {
                notifyError("Unable to read file: " + file);
            } else {
                parser = new FileDataParser(file);
                readSample(true);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            notifyError("Error opening file: " + ex.getMessage());
        }
    }

    private void readSample(boolean forward) {
        new FileOpener(forward).execute();
    }

    public void addListener(SampleSelectionListener listener) {
        listeners.add(listener);
    }

    private void relayStats(String file, String time, String type, Map<Stack,StatisticAdder> data) {
        for(SampleSelectionListener listener : listeners) {
            listener.sampleWasSelected(file, time, type, data);
        }
    }

    private class FileOpener extends SwingWorker<Map<Stack,StatisticAdder>,Object> {
        private final boolean forward;
        public FileOpener(boolean forward) {
            this.forward = forward;
        }
        @Override
        public Map<Stack,StatisticAdder> doInBackground() throws Exception {
            return forward ? parser.getNextSample() : parser.getPreviousSample();
        }
        @Override
        public void done() {
            try {
                final Map<Stack,StatisticAdder> sample = get();
                if(sample!=null) {
                    final Date time = parser.getSampleTime();
                    final String sampleTime = hms.format(time);
                    relayStats(parser.getFilename(), parser.getSampleType(), sampleTime, sample);
                }
            } catch(Exception ex) {
                notifyError("Parse error reading file");
            }
        }
    }

    /////

    public void nextSample() {
        readSample(true);
    }

    public void previousSample() {
        readSample(false);
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    private void notifyContentsChanged() {
        for(SampleSelectionListener listener : listeners) {
            listener.contentsChanged(this);
        }
    }
}
