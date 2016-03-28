package com.dell.gumshoe.tools;

import com.dell.gumshoe.Probe;
import com.dell.gumshoe.socket.SocketIOListener.DetailAccumulator;
import com.dell.gumshoe.socket.SocketIOStackReporter.Listener;
import com.dell.gumshoe.stack.Stack;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ProbeSourcePanel extends JPanel implements Listener {
    private static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");

    private Map<Stack,DetailAccumulator> lastReceivedStats;
    private String receiveTime;
    private final StatisticsSourcePanel parent;

    private final JCheckBox sendLive = new JCheckBox("Display as soon as received");
    private final JButton sendNow = new JButton("Update");
    private final JLabel received = new JLabel("No data has been received");

    public ProbeSourcePanel(StatisticsSourcePanel parent, Probe probe) {
        this.parent = parent;
        sendNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                relayStats();
            }
        });

        sendLive.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendNow.setEnabled(sendLive.isSelected() && lastReceivedStats!=null);;
            }
        });

        setLayout(new GridLayout(4,1));
        add(received);
        add(sendLive);
        add(sendNow);

        if(probe!=null && probe.getIOReporter()!=null) {
            probe.getIOReporter().addListener(this);
        }
    }

    @Override
    public void socketIOStatsReported(Map<Stack, DetailAccumulator> stats) {
        if(stats.size()>0) {
            receiveTime = hms.format(new Date());
            received.setText("Received data " + receiveTime);
            lastReceivedStats = stats;
            if(sendLive.isSelected()) {
                relayStats();
            } else {
                sendNow.setEnabled(true);
            }
        }
    }

    private void relayStats() {
        if(lastReceivedStats!=null) {
            parent.setStatus("Probe sample time: " + receiveTime);
            parent.setSample(lastReceivedStats);
            lastReceivedStats = null;
            sendNow.setEnabled(false);
        }
    }
}
