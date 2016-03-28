package com.dell.gumshoe.tools;

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

public class StatisticsRelay extends JPanel implements Listener {
    private static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");
    private Map<Stack,DetailAccumulator> lastStats;
    private String receiveTime;
    private final FlameGraph target;
    private final JCheckBox sendLive = new JCheckBox("Display as received");
    private final JButton sendNow = new JButton("Update");
    private final JLabel received = new JLabel("No data received");
    private final JLabel display = new JLabel("No data displayed");

    public StatisticsRelay(FlameGraph target) {
        this.target = target;
        sendNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                relayStats();
            }
        });

        setLayout(new GridLayout(4,1));
        add(received);
        add(display);
        add(sendLive);
        add(sendNow);
    }

    @Override
    public void socketIOStatsReported(Map<Stack, DetailAccumulator> stats) {
        debug("stats received " + stats.size());
        if(stats.size()>0) {
            receiveTime = hms.format(new Date());
            received.setText("Received data " + receiveTime);
            lastStats = stats;
            if(sendLive.isSelected()) {
                relayStats();
            } else {
                sendNow.setEnabled(true);
            }
        }
    }

    private void relayStats() {
        if(lastStats!=null) {
            display.setText("Displaying " + receiveTime);
            target.updateModel(lastStats);
            sendNow.setEnabled(false);
        }
    }

    private static void debug(String msg) {
        System.out.println(msg);
    }
}
