package com.dell.gumshoe.tools;

import static com.dell.gumshoe.tools.Swing.columns;
import static com.dell.gumshoe.tools.Swing.flow;
import static com.dell.gumshoe.tools.Swing.groupButtons;
import static com.dell.gumshoe.tools.Swing.stackNorth;
import static com.dell.gumshoe.tools.Swing.stackSouth;
import static com.dell.gumshoe.tools.Swing.stackWest;
import static com.dell.gumshoe.tools.Swing.titled;

import com.dell.gumshoe.tools.graph.DisplayOptions;
import com.dell.gumshoe.tools.stats.DataTypeHelper;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OptionEditor extends JPanel {
    private final JRadioButton byCaller = new JRadioButton("show callers (root graph)", true);
    private final JRadioButton byCalled = new JRadioButton("show called methods (flame graph)");
    private final JRadioButton valueWidth = new JRadioButton("statistic value", true);
    private final JRadioButton logWidth = new JRadioButton("log(value)");
    private final JRadioButton equalWidth = new JRadioButton("equal width");
    private final JCheckBox byValue = new JCheckBox("arrange by statistic value (left to right)");
    private final JTextField statLimit = new JTextField();
    private final JButton apply = new JButton("Apply");
    private final JComboBox statSelector = new JComboBox(DataTypeHelper.getTypes().toArray());
    private final CardLayout statCard = new CardLayout();
    private final JPanel statOptions = new JPanel();

    public OptionEditor() {
        groupButtons(byCalled, byCaller);
        final JPanel directionPanel = columns(new JLabel("Direction: "), byCaller, byCalled, new JLabel(""));

        groupButtons(valueWidth, logWidth, equalWidth);
        final JPanel widthPanel = columns(new JLabel("Cell width: "), valueWidth, logWidth, equalWidth);


        statLimit.setColumns(3);
        final JPanel displaySettingsPanel = columns(
                stackWest(new JLabel("Drop frames less than"), statLimit, new JLabel("%")),
                byValue);

        final JPanel graphPanel = titled("Graph generation options",
                stackNorth(directionPanel, widthPanel, displaySettingsPanel));

        /////

        statSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String label = (String)statSelector.getSelectedItem();
                statCard.show(statOptions, label);
            }
        });
        final JPanel statChooserPanel = stackWest(new JLabel("For sample type "), statSelector, new JLabel(":"));
        statOptions.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        statOptions.setLayout(statCard);
        for(String typeName : DataTypeHelper.getTypes()) {
            statOptions.add(DataTypeHelper.forType(typeName).getOptionEditor(), typeName);
        }

        final JPanel bottomPanel = stackSouth(flow(apply), graphPanel);
        final JPanel statPanel = new JPanel();
        statPanel.setLayout(new BorderLayout());
        statPanel.add(statChooserPanel, BorderLayout.NORTH);
        statPanel.add(statOptions, BorderLayout.CENTER);
        titled("Select statistic to display", statPanel);

        setLayout(new BorderLayout());
        add(bottomPanel, BorderLayout.SOUTH);
        add(statPanel, BorderLayout.CENTER);
    }

    public void addActionListener(ActionListener listener) {
        apply.addActionListener(listener);
        listener.actionPerformed(new ActionEvent(this, 0, ""));
    }

    public DisplayOptions getOptions() {
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
        return new DisplayOptions(isInverted, order, width, minPct);
    }
}
