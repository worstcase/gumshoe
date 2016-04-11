package com.dell.gumshoe.tools;

import com.dell.gumshoe.tools.graph.DisplayOptions;
import com.dell.gumshoe.tools.graph.StackGraphPanel.IOStat;
import com.dell.gumshoe.tools.graph.StackGraphPanel.IOUnit;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OptionEditor extends JPanel {
    final JRadioButton readStat = new JRadioButton("read", true);
    final JRadioButton writeStat = new JRadioButton("write");
    final JRadioButton bothStat = new JRadioButton("read+write");
    final JRadioButton opsUnit = new JRadioButton("ops", true);
    final JRadioButton bytesUnit = new JRadioButton("bytes");
    final JRadioButton timeUnit = new JRadioButton("time(ms)");
    final JRadioButton byCaller = new JRadioButton("show callers (root graph)", true);
    final JRadioButton byCalled = new JRadioButton("show called methods (flame graph)");
    final JRadioButton valueWidth = new JRadioButton("statistic value", true);
    final JRadioButton logWidth = new JRadioButton("log(value)");
    final JRadioButton equalWidth = new JRadioButton("equal width");
    final JCheckBox byValue = new JCheckBox("arrange by value");
    private final JTextField statLimit = new JTextField();
    final JButton apply = new JButton("Apply");

    public OptionEditor() {
        final ButtonGroup statGroup = new ButtonGroup();
        statGroup.add(readStat);
        statGroup.add(writeStat);
        statGroup.add(bothStat);

        final JPanel statPanel = new JPanel();
        statPanel.setLayout(new GridLayout(1,3));
        statPanel.add(readStat);
        statPanel.add(writeStat);
        statPanel.add(bothStat);
        statPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Operation", TitledBorder.LEFT, TitledBorder.TOP));


        final ButtonGroup unitGroup = new ButtonGroup();
        unitGroup.add(opsUnit);
        unitGroup.add(bytesUnit);
        unitGroup.add(timeUnit);

        final JPanel unitPanel = new JPanel();
        unitPanel.setLayout(new GridLayout(1,3));
        unitPanel.add(opsUnit);
        unitPanel.add(bytesUnit);
        unitPanel.add(timeUnit);
        unitPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Measurement", TitledBorder.LEFT, TitledBorder.TOP));

        statLimit.setColumns(3);
        final JPanel minPanel = new JPanel();
        minPanel.setLayout(new BorderLayout());
        minPanel.add(new JLabel("Drop frames less than"), BorderLayout.WEST);
        minPanel.add(statLimit, BorderLayout.CENTER);
        minPanel.add(new JLabel("%"), BorderLayout.EAST);

        final ButtonGroup widthGroup = new ButtonGroup();
        widthGroup.add(valueWidth);
        widthGroup.add(logWidth);
        widthGroup.add(equalWidth);

        final JPanel widthPanel = new JPanel();
        widthPanel.setLayout(new GridLayout(1,3));
        widthPanel.add(valueWidth);
        widthPanel.add(logWidth);
        widthPanel.add(equalWidth);
        widthPanel.add(minPanel);
        widthPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Cell width", TitledBorder.LEFT, TitledBorder.TOP));

        final ButtonGroup directionGroup = new ButtonGroup();
        directionGroup.add(byCalled);
        directionGroup.add(byCaller);

        final JPanel directionPanel = new JPanel();
        directionPanel.setLayout(new GridLayout(1,2));
        directionPanel.add(byCalled);
        directionPanel.add(byCaller);
        directionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Direction", TitledBorder.LEFT, TitledBorder.TOP));

        final JPanel otherPanel = new JPanel();
        otherPanel.setLayout(new FlowLayout());
        otherPanel.add(byValue);
        otherPanel.add(apply);

        setLayout(new GridLayout(5,1));
        add(statPanel);
        add(unitPanel);
        add(directionPanel);
        add(widthPanel);
        add(otherPanel);
    }

    public void addActionListener(ActionListener listener) {
        apply.addActionListener(listener);
        listener.actionPerformed(new ActionEvent(this, 0, ""));
    }

    public DisplayOptions getOptions() {
        final IOStat stat;
        if(readStat.isSelected()) stat = IOStat.READ;
        else if(writeStat.isSelected()) stat = IOStat.WRITE;
        else stat = IOStat.READ_PLUS_WRITE;

        final IOUnit unit;
        if(opsUnit.isSelected()) unit = IOUnit.OPS;
        else if(bytesUnit.isSelected()) unit = IOUnit.BYTES;
        else unit = IOUnit.TIME;

        final DisplayOptions.Order order = byValue.isSelected() ? DisplayOptions.Order.BY_VALUE : DisplayOptions.Order.BY_NAME;

        final DisplayOptions.WidthScale width;
        if(valueWidth.isSelected()) width = DisplayOptions.WidthScale.VALUE;
        else if(logWidth.isSelected()) width = DisplayOptions.WidthScale.LOG_VALUE;
        else width = DisplayOptions.WidthScale.EQUAL;

        final boolean isInverted = byCalled.isSelected();
        float minPct = 0f;
        try {
            minPct = Float.parseFloat(statLimit.getText());
        } catch(Exception ignore) {

        }
        return new DisplayOptions(isInverted, stat, unit, order, width, minPct);
    }
}
