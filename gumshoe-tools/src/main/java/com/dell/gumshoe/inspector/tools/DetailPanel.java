package com.dell.gumshoe.inspector.tools;

import static com.dell.gumshoe.util.Swing.flow;
import static com.dell.gumshoe.util.Swing.stackNorth;

import com.dell.gumshoe.inspector.graph.StackFrameNode;
import com.dell.gumshoe.inspector.graph.StackGraphPanel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.Set;

public class DetailPanel extends JPanel implements HasCloseButton, ListSelectionListener {
    private static final String PROTOTYPE_FRAME = "com.dell.gumshoe.thread.ThreadMonitor$ThreadDumper.performDumpAndReschedule(ThreadMonitor.java:248)";
    private final JLabel frame = new JLabel();
    private final JLabel callType = new JLabel("Call:");
    private final JTextArea stats = new JTextArea();
    final JButton close = new JButton("Close");

    final DefaultListModel callModel = new DefaultListModel();
    final DefaultListModel contextModel = new DefaultListModel();

    public DetailPanel() {
        super(new BorderLayout());

        final JList contextList = new JList(contextModel);
        contextList.setVisibleRowCount(8);
        contextList.setPrototypeCellValue(PROTOTYPE_FRAME);
        final JList callList = new JList(callModel);
        callList.setVisibleRowCount(5);
        callList.setPrototypeCellValue(PROTOTYPE_FRAME);

        final JPanel frameRow = new JPanel(new BorderLayout());
        frameRow.add(new JLabel("Stack frame:"), BorderLayout.WEST);
        frameRow.add(frame, BorderLayout.CENTER);

        final JPanel frameInfo = stackNorth(
                frameRow,
                new JLabel(" "),
                new JLabel("Context:"), new JScrollPane(contextList),
                new JLabel(),
                callType, new JScrollPane(callList),
                new JLabel(" "),
                new JLabel("Probe stats:"));
        frameInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        stats.setRows(15);
        add(frameInfo, BorderLayout.NORTH);
        add(stats, BorderLayout.CENTER);
        add(flow(close), BorderLayout.SOUTH);
    }

    public void setModel(StackFrameNode boxNode, StackFrameNode parentNode, String statInfo) {
        frame.setText(boxNode.getFrame().toString());

        contextModel.clear();
        for(StackTraceElement frame : boxNode.getContext()) {
            contextModel.addElement(frame);
        }

        final boolean byCalled = boxNode.isByCalled();
        callType.setText(byCalled ? "Calls into:" : "Called by:");
        final Set<StackTraceElement> callFrames = byCalled ? boxNode.getCalledFrames() : boxNode.getCallingFrames();
        callModel.clear();
        for(StackTraceElement frame : callFrames) {
            callModel.addElement(frame);
        }

        stats.setText(statInfo);
    }

    @Override
    public void addCloseListener(ActionListener listener) {
        close.addActionListener(listener);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        final StackGraphPanel graph = (StackGraphPanel) e.getSource();
        final StackFrameNode boxNode = graph.getSelection();
        final StackFrameNode parentNode = graph.getSelectionParent();
        final String statInfo = boxNode.getStats(parentNode);
        setModel(boxNode, parentNode, statInfo);
    }

}
