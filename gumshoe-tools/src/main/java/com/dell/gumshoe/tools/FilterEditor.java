package com.dell.gumshoe.tools;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.stack.Filter;
import com.dell.gumshoe.stack.Filter.Builder;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.tools.graph.StackGraphPanel;
import static com.dell.gumshoe.tools.Swing.*;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class FilterEditor extends JPanel {
    private final JRadioButton dropZero = new JRadioButton("drop the stack");
    private final JRadioButton revertZero = new JRadioButton("keep all frames");
    private final JRadioButton bucketZero = new JRadioButton("group as \"other\"", true);

    private final JTextField topCount = new JTextField();
    private final JTextField bottomCount = new JTextField();
    private final JLabel topLabel1 = new JLabel("Top");
    private final JLabel topLabel2 = new JLabel("frames");
    private final JLabel bothLabel = new JLabel(" and ");
    private final JLabel bottomLabel1 = new JLabel("bottom");
    private final JLabel bottomLabel2 = new JLabel("frames");

    private final JCheckBox dropJVM = new JCheckBox("drop jdk and gumshoe frames", true);

    private final JTextArea accept = new JTextArea();
    private final JTextArea reject = new JTextArea();

    private final JButton localButton = new JButton("Apply");
    private final JButton generate = new JButton("Generate cmdline options");

    private StackGraphPanel graph;

    public FilterEditor() {
        groupButtons(revertZero, bucketZero);
        final JPanel zeroPanel = titled("When stack filter matches no frames:", flow(revertZero, bucketZero));

        topCount.setColumns(3);
        bottomCount.setColumns(3);
        final JPanel countPanel = titled("Retain only:",
                flow(topLabel1, topCount, topLabel2, bothLabel, bottomLabel1, bottomCount, bottomLabel2));

        // lighten/darken words to help make filter intent more clear
        topLabel1.setEnabled(false);
        topLabel2.setEnabled(false);
        bothLabel.setEnabled(false);
        bottomLabel1.setEnabled(false);
        bottomLabel2.setEnabled(false);
        topCount.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                final boolean topPositive = isPositive(topCount);
                topLabel1.setEnabled(topPositive);
                topLabel2.setEnabled(topPositive);
                bothLabel.setEnabled(topPositive && isPositive(bottomCount));
            } });
        bottomCount.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                final boolean bottomPositive = isPositive(bottomCount);
                bottomLabel1.setEnabled(bottomPositive);
                bottomLabel2.setEnabled(bottomPositive);
                bothLabel.setEnabled(bottomPositive && isPositive(topCount));
            } });

        final JPanel acceptPanel = titled("Include classes:", accept);
        final JPanel rejectPanel = titled("Exclude classes:", reject);

        final JPanel jvmPanel = flow(dropJVM);

        localButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGraph();
            }
        });

        stackNorth(this, zeroPanel, countPanel, acceptPanel, rejectPanel, jvmPanel, flow(localButton));
    }

    public void setGraph(StackGraphPanel graph) {
        this.graph = graph;
    }

    private boolean isPositive(JTextField field) {
        final String textValue = field.getText().trim();
        boolean validPositiveNumber = false;
        try {
            int value = Integer.parseInt(textValue);
            if(value>0) {
                validPositiveNumber = true;
            }
        } catch(Exception ignore) { }
        return validPositiveNumber;
    }

    private int getCount(JTextField field) {
        try {
            final int value = Integer.parseInt(field.getText().trim());
            if(value>0) {
                return value;
            }
        } catch(Exception ignore) { }
        return 0;
    }

    public StackFilter getFilter() {
        final Builder builder = Filter.builder();
        builder.withEndsOnly(getCount(topCount), getCount(bottomCount));
        if(dropJVM.isSelected()) { builder.withExcludePlatform(); }
        if(revertZero.isSelected()) { builder.withOriginalIfBlank(); }
        for(String acceptLine : getValues(accept)) { builder.withOnlyClasses(acceptLine); }
        for(String rejectLine : getValues(reject)) { builder.withExcludeClasses(rejectLine); }
        return builder.build();
    }

    private List<String> getValues(JTextArea field) {
        final String rawValue = field.getText();
        final String[] lines = rawValue.split("\n");
        final List<String> out = new ArrayList<>(lines.length);
        for(String line : lines) {
            final String clean = line.trim();
            if(clean.length()>0) {
                out.add(clean);
            }
        }
        return out;
    }

    private void updateGraph() {
        if(graph!=null) {
            final StackFilter filter = getFilter();
            graph.setFilter(filter);
        }
    }
}
