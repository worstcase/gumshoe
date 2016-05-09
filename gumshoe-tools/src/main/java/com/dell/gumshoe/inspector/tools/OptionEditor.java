package com.dell.gumshoe.inspector.tools;

import static com.dell.gumshoe.util.Swing.columns;
import static com.dell.gumshoe.util.Swing.flow;
import static com.dell.gumshoe.util.Swing.groupButtons;
import static com.dell.gumshoe.util.Swing.stackNorth;
import static com.dell.gumshoe.util.Swing.stackSouth;
import static com.dell.gumshoe.util.Swing.stackWest;
import static com.dell.gumshoe.util.Swing.titled;

import com.dell.gumshoe.inspector.graph.DisplayOptions;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

public class OptionEditor extends JPanel implements HasCloseButton {
    private final JRadioButton byCaller = new JRadioButton("show callers (root graph)", true);
    private final JRadioButton byCalled = new JRadioButton("show called methods (flame graph)");
    private final JRadioButton valueWidth = new JRadioButton("statistic value", true);
    private final JRadioButton logWidth = new JRadioButton("log(value)");
    private final JRadioButton equalWidth = new JRadioButton("equal width");
    private final JCheckBox byValue = new JCheckBox("arrange by statistic value (left to right)");
    private final JTextField statLimit = new JTextField();
    private final JButton apply = new JButton("OK");

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

        final JPanel bottomPanel = stackSouth(flow(apply), graphPanel);

        setLayout(new BorderLayout());
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void addActionListener(ActionListener listener) {
        apply.addActionListener(listener);
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

    @Override
    public void addCloseListener(ActionListener listener) {
        apply.addActionListener(listener);
    }
}
