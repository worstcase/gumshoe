package com.dell.gumshoe.inspector.tools;

import static com.dell.gumshoe.util.Swing.flow;
import static com.dell.gumshoe.util.Swing.groupButtons;
import static com.dell.gumshoe.util.Swing.rows;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.inspector.ReportSource;
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

public class ProbeSourcePanel extends JPanel implements Listener<StatisticAdder>, ReportSource, HasCloseButton {
    private static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");

    private List<ActionListener> closeListeners = new CopyOnWriteArrayList<>();
    private final List<ReportSelectionListener> listeners = new CopyOnWriteArrayList<>();
    private final JRadioButton ignoreIncoming = new JRadioButton("drop new reports");
    private final JRadioButton dropOldest = new JRadioButton("drop oldest report", true);
    private final JCheckBox sendLive = new JCheckBox("Immediately view newest");
    private final JButton sendNow = new JButton("View report");
    private final JButton ok = new JButton("OK");
    private int reportCount = 3;
    private final DefaultListModel reportModel = new DefaultListModel();
    private final JList reportList = new JList(reportModel);

    public ProbeSourcePanel(ProbeManager probe) {
        super(new BorderLayout());

        sendNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Report selected = (Report)reportList.getSelectedValue();
                if(selected!=null) {
                    relayStats(selected);
                }
            }
        });

        reportList.setVisibleRowCount(reportCount);
        reportList.setPrototypeCellValue(new Report("socket-io", new Date(), Collections.<Stack,StatisticAdder>emptyMap()));
        reportList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if( ! sendNow.isSelected() && e.getClickCount()==2) {
                    int index = reportList.locationToIndex(e.getPoint());
                    final Report selected = (Report)reportModel.getElementAt(index);
                    if(selected!=null) {
                        relayStats(selected);
                    }
                 }
            }
        });

        // row: which types of data to receive
        final JPanel acceptPanel = new JPanel();
        acceptPanel.add(new JLabel("Accept reports from probes:"));
        for(String type : DataTypeHelper.getTypes()) {
            acceptPanel.add(DataTypeHelper.forType(type).getSelectionComponent());
        }

        // row: how to handle full buffer
        final JTextField retainCount = new JTextField();
        retainCount.setColumns(3);
        retainCount.setText(Integer.toString(reportCount));
        groupButtons(ignoreIncoming, dropOldest);
        final JLabel fullLabel = new JLabel("Retain full:");
        final JPanel handleIncoming = flow(new JLabel("Retain"), retainCount, new JLabel("reports, then: "), ignoreIncoming, dropOldest);

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
        add(new JScrollPane(reportList, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        add(flow(sendNow, sendNow, ok), BorderLayout.SOUTH);

        if(probe!=null) {
            for(String helperType : DataTypeHelper.getTypes()) {
                DataTypeHelper.forType(helperType).addListener(probe, this);
            }
        }
    }

    private boolean canAccept(String reportType, Map<Stack,StatisticAdder> stats) {
        if(stats.isEmpty()) { return false; }

        // see if the box is checked to accept this report type
        for(String helperType : DataTypeHelper.getTypes()) {
            if(helperType.equals(reportType)) {
                if(DataTypeHelper.forType(helperType).isSelected()) {
                    break;
                } else {
                    return false;
                }
            }
        }

        // see if buffer has room or we can remove one
        if(reportModel.size()>=reportCount) {
            if(ignoreIncoming.isSelected() && ! sendLive.isSelected()) { return false; }
            reportModel.removeElementAt(0);
        }
        return true;
    }

    /////

    @Override
    public synchronized void statsReported(String type, Map<Stack,StatisticAdder> stats) {
        if(canAccept(type, stats)) {
            final Date date = new Date();
            final Report report = new Report(type, date, stats);
            if(sendLive.isSelected()) {
                reportModel.clear();
            }
            reportModel.addElement(report);
            if(sendLive.isSelected()) {
                relayStats(report);
            }
            notifyContentsChanged();
       }
    }

    private void relayStats(Report report) {
        for(ReportSelectionListener listener : listeners) {
            listener.reportWasSelected(this, report.time, report.type, report.data);
        }
        notifyCloseListeners();
    }

    public void addListener(ReportSelectionListener listener) {
        listeners.add(listener);
    }

    private static class Report {
        String label;
        String time;
        String type;
        Map<Stack,StatisticAdder> data;

        public Report(String type, Date reportTime, Map<Stack,StatisticAdder> data) {
            this.time =  hms.format(reportTime);
            this.label = time + " " + type + ": " + DataTypeHelper.forType(type).getSummary(data);
            this.type = type;
            this.data = data;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public void nextReport() {
        final int size = reportModel.getSize();
        if(size==0) { return; }

        final int currentIndex = reportList.getSelectedIndex();
        if(currentIndex>=0) {
            reportList.setSelectedIndex(currentIndex+1);
            relayStats((Report) reportList.getSelectedValue());
        } else {
            reportList.setSelectedIndex(0);
            relayStats((Report) reportList.getSelectedValue());
        }
    }

    public void previousReport() {
        final int size = reportModel.getSize();
        if(size==0) { return; }

        final int currentIndex = reportList.getSelectedIndex();
        if(currentIndex>=0) {
            reportList.setSelectedIndex(currentIndex-1);
            relayStats((Report) reportList.getSelectedValue());
        } else {
            reportList.setSelectedIndex(size-1);
            relayStats((Report) reportList.getSelectedValue());
        }
    }

    @Override
    public boolean hasNext() {
        if(sendLive.isSelected()) { return false; }
        final int size = reportModel.getSize();
        final int selectedIndex = reportList.getSelectedIndex();
        if(selectedIndex==-1) { return size>0; }
        return size > selectedIndex+1;
    }

    @Override
    public boolean hasPrevious() {
        if(sendLive.isSelected()) { return false; }
        final int size = reportModel.getSize();
        final int selectedIndex = reportList.getSelectedIndex();
        if(selectedIndex==-1) { return size>0; }
        return reportList.getSelectedIndex()>0;
    }

    private void notifyContentsChanged() {
        for(ReportSelectionListener listener : listeners) {
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
