package com.dell.gumshoe.tools;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter.Listener;
import com.dell.gumshoe.tools.stats.DataTypeHelper;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ProbeSourcePanel extends JPanel implements Listener<StatisticAdder> {
    private static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");

    private String lastReceivedType;
    private Map<Stack,StatisticAdder> lastReceivedStats;
    private String receiveTime;
    private final StatisticsSourcePanel parent;

    private final JCheckBox acceptSocketIO = new JCheckBox("socket IO", true);
    private final JCheckBox acceptSocketUnclosed = new JCheckBox("unclosed sockets");
//    private final JCheckBox acceptFileIO = new JCheckBox("file IO");
    private final JRadioButton ignoreIncoming = new JRadioButton("ignore new samples");
    private final JRadioButton dropOldest = new JRadioButton("drop oldest sample", true);
    private final JCheckBox sendLive = new JCheckBox("Immediately show newest");
    private final JButton sendNow = new JButton("Display selected");
    private final JLabel received = new JLabel("No data has been received");
    private int sampleCount = 3;
    private final DefaultListModel sampleModel = new DefaultListModel();
    private final JList sampleList = new JList(sampleModel);

    public ProbeSourcePanel(StatisticsSourcePanel parent, Probe probe) {
        this.parent = parent;

        sendNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Sample selected = (Sample)sampleList.getSelectedValue();
                if(selected!=null) {
                    relayStats(selected);
                }
            }
        });

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

        final JPanel acceptPanel = new JPanel();
        acceptPanel.add(new JLabel("Accept:"));
        acceptPanel.add(acceptSocketIO);
        acceptPanel.add(acceptSocketUnclosed);
//        acceptPanel.add(acceptFileIO);

        final ButtonGroup fullGroup = new ButtonGroup();
        fullGroup.add(ignoreIncoming);
        fullGroup.add(dropOldest);

        final JPanel handleIncoming = new JPanel();
        handleIncoming.add(sendLive);
        final JLabel fullLabel = new JLabel("When buffer full:");
        handleIncoming.add(fullLabel);
        handleIncoming.add(ignoreIncoming);
        handleIncoming.add(dropOldest);

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

        final JPanel optionPanel = new JPanel();
        optionPanel.setLayout(new GridLayout(3,1));
        optionPanel.add(received);
        optionPanel.add(acceptPanel);
        optionPanel.add(handleIncoming);

        JScrollPane sampleScroll = new JScrollPane(sampleList);

        setLayout(new BorderLayout());
        add(optionPanel, BorderLayout.NORTH);
        add(sampleScroll, BorderLayout.CENTER);

        if(probe!=null) {
            if(probe.getIOReporter()!=null) {
                probe.getIOReporter().addListener(this);
            }
            if(probe.getUnclosedReporter()!=null) {
                probe.getUnclosedReporter().addListener(this);
            }
        }
    }

    private boolean canAccept(String type, Map<Stack,StatisticAdder> stats) {
        if(stats.isEmpty()) { return false; }

        if(Probe.SOCKET_IO_LABEL.equals(type) && ! acceptSocketIO.isSelected()) { return false; }
        if(Probe.UNCLOSED_SOCKET_LABEL.equals(type) && ! acceptSocketUnclosed.isSelected()) { return false; }

        // check if buffer full and if we can remove one
        if(sampleModel.size()>=sampleCount) {
            if(ignoreIncoming.isSelected() && ! sendLive.isSelected()) { return false; }
            sampleModel.removeElementAt(0);
        }
        return true;
    }

    @Override
    public synchronized void statsReported(String type, Map<Stack,StatisticAdder> stats) {
        if(canAccept(type, stats)) {
            final Date date = new Date();
            receiveTime = hms.format(date);
            final Sample sample = new Sample(type, date, stats);
            if(sendLive.isSelected()) {
                sampleModel.clear();
            }
            sampleModel.addElement(sample);
            received.setText("Received data " + receiveTime);
            if(sendLive.isSelected()) {
                relayStats(sample);
            }
        }
    }

    private void relayStats(Sample sample) {
        parent.setStatus("Displaying sample: " + sample.time + " " + sample.label);
        parent.setSample(sample.data);
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
}
