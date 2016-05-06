package com.dell.gumshoe.inspector.tools;

import static com.dell.gumshoe.util.Swing.flow;
import static com.dell.gumshoe.util.Swing.groupButtons;
import static com.dell.gumshoe.util.Swing.rows;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.inspector.SampleSource;
import com.dell.gumshoe.inspector.helper.DataTypeHelper;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter.Listener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProbeSourcePanel extends JPanel implements Listener<StatisticAdder>, SampleSource, HasCloseButton {
    private static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");

    private List<ActionListener> closeListeners = new CopyOnWriteArrayList<>();
    private final List<SampleSelectionListener> listeners = new CopyOnWriteArrayList<>();
    private final JRadioButton ignoreIncoming = new JRadioButton("drop new samples");
    private final JRadioButton dropOldest = new JRadioButton("drop oldest sample", true);
    private final JCheckBox sendLive = new JCheckBox("Immediately view newest");
    private final JButton sendNow = new JButton("View sample");
    private final JButton ok = new JButton("OK");
    private int sampleCount = 3;
    private final DefaultListModel sampleModel = new DefaultListModel();
    private final JList sampleList = new JList(sampleModel);

    public ProbeSourcePanel(ProbeManager probe) {
        super(new BorderLayout());

        sendNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Sample selected = (Sample)sampleList.getSelectedValue();
                if(selected!=null) {
                    relayStats(selected);
                }
            }
        });

        sampleList.setVisibleRowCount(sampleCount);
        sampleList.setPrototypeCellValue(new Sample("socket-io", new Date(), Collections.<Stack,StatisticAdder>emptyMap()));
        sampleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if( ! sendNow.isSelected() && e.getClickCount()==2) {
                    int index = sampleList.locationToIndex(e.getPoint());
                    final Sample selected = (Sample)sampleModel.getElementAt(index);
                    if(selected!=null) {
                        relayStats(selected);
                    }
                 }
            }
        });

        // row: which types of data to receive
        final JPanel acceptPanel = new JPanel();
        acceptPanel.add(new JLabel("Accept samples from probes:"));
        for(String type : DataTypeHelper.getTypes()) {
            acceptPanel.add(DataTypeHelper.forType(type).getSelectionComponent());
        }

        // row: how to handle full buffer
        final JTextField retainCount = new JTextField();
        retainCount.setColumns(3);
        retainCount.setText(Integer.toString(sampleCount));
        groupButtons(ignoreIncoming, dropOldest);
        final JLabel fullLabel = new JLabel("Retain full:");
        final JPanel handleIncoming = flow(new JLabel("Retain"), retainCount, new JLabel("samples, then: "), ignoreIncoming, dropOldest);

        sendLive.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean manualMode = ! sendLive.isSelected();
                sendNow.setEnabled(manualMode);
                fullLabel.setEnabled(manualMode);
                ignoreIncoming.setEnabled(manualMode);
                dropOldest.setEnabled(manualMode);
            }
        });


        setLayout(new BorderLayout());
        add(rows(acceptPanel, handleIncoming), BorderLayout.NORTH);
        add(new JScrollPane(sampleList, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        add(flow(sendNow, sendNow, ok), BorderLayout.SOUTH);

        if(probe!=null) {
            for(String helperType : DataTypeHelper.getTypes()) {
                DataTypeHelper.forType(helperType).addListener(probe, this);
            }
        }
    }

    private boolean canAccept(String sampleType, Map<Stack,StatisticAdder> stats) {
        if(stats.isEmpty()) { return false; }

        // see if the box is checked to accept this sample type
        for(String helperType : DataTypeHelper.getTypes()) {
            if(helperType.equals(sampleType)) {
                if(DataTypeHelper.forType(helperType).isSelected()) {
                    break;
                } else {
                    return false;
                }
            }
        }

        // see if buffer has room or we can remove one
        if(sampleModel.size()>=sampleCount) {
            if(ignoreIncoming.isSelected() && ! sendLive.isSelected()) { return false; }
            sampleModel.removeElementAt(0);
        }
        return true;
    }

    /////

    @Override
    public synchronized void statsReported(String type, Map<Stack,StatisticAdder> stats) {
        if(canAccept(type, stats)) {
            final Date date = new Date();
            final Sample sample = new Sample(type, date, stats);
            if(sendLive.isSelected()) {
                sampleModel.clear();
            }
            sampleModel.addElement(sample);
            if(sendLive.isSelected()) {
                relayStats(sample);
            }
            notifyContentsChanged();
       }
    }

    private void relayStats(Sample sample) {
        for(SampleSelectionListener listener : listeners) {
            listener.sampleWasSelected(this, sample.time, sample.type, sample.data);
        }
        notifyCloseListeners();
    }

    public void addListener(SampleSelectionListener listener) {
        listeners.add(listener);
    }

    private static class Sample {
        String label;
        String time;
        String type;
        Map<Stack,StatisticAdder> data;

        public Sample(String type, Date sampleTime, Map<Stack,StatisticAdder> data) {
            this.time =  hms.format(sampleTime);
            this.label = time + " " + type + ": " + DataTypeHelper.forType(type).getSummary(data);
            this.type = type;
            this.data = data;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public void nextSample() {
        final int size = sampleModel.getSize();
        if(size==0) { return; }

        final int currentIndex = sampleList.getSelectedIndex();
        if(currentIndex>=0) {
            sampleList.setSelectedIndex(currentIndex+1);
            relayStats((Sample) sampleList.getSelectedValue());
        } else {
            sampleList.setSelectedIndex(0);
            relayStats((Sample) sampleList.getSelectedValue());
        }
    }

    public void previousSample() {
        final int size = sampleModel.getSize();
        if(size==0) { return; }

        final int currentIndex = sampleList.getSelectedIndex();
        if(currentIndex>=0) {
            sampleList.setSelectedIndex(currentIndex-1);
            relayStats((Sample) sampleList.getSelectedValue());
        } else {
            sampleList.setSelectedIndex(size-1);
            relayStats((Sample) sampleList.getSelectedValue());
        }
    }

    @Override
    public boolean hasNext() {
        if(sendLive.isSelected()) { return false; }
        final int size = sampleModel.getSize();
        final int selectedIndex = sampleList.getSelectedIndex();
        if(selectedIndex==-1) { return size>0; }
        return size > selectedIndex+1;
    }

    @Override
    public boolean hasPrevious() {
        if(sendLive.isSelected()) { return false; }
        final int size = sampleModel.getSize();
        final int selectedIndex = sampleList.getSelectedIndex();
        if(selectedIndex==-1) { return size>0; }
        return sampleList.getSelectedIndex()>0;
    }

    private void notifyContentsChanged() {
        for(SampleSelectionListener listener : listeners) {
            listener.contentsChanged(this);
        }
    }

    @Override
    public void addCloseListener(ActionListener listener) {
        closeListeners.add(listener);
        ok.addActionListener(listener);
    }

    private void notifyCloseListeners() {
        for(ActionListener listener : closeListeners) {
            listener.actionPerformed(new ActionEvent(this, 0, ""));
        }
    }
}
