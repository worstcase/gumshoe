package com.dell.gumshoe.tools;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.stack.Filter;
import com.dell.gumshoe.stack.Filter.Builder;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.tools.graph.StackGraphPanel;

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

    private final JButton localButton = new JButton("Apply to display");
    private final JButton probeButton = new JButton("Apply to probe");
    private final JButton generate = new JButton("Generate cmdline options");

    private ProbeManager probe;
    private StackGraphPanel graph;

    public FilterEditor() {
        ButtonGroup zeroGroup = new ButtonGroup();
//        zeroGroup.add(dropZero);
        zeroGroup.add(revertZero);
        zeroGroup.add(bucketZero);

        final JPanel zeroPanel = new JPanel();
        zeroPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "When stack filter matches no frames:", TitledBorder.LEFT, TitledBorder.TOP));
//        zeroPanel.add(dropZero);
        zeroPanel.add(revertZero);
        zeroPanel.add(bucketZero);

        topCount.setColumns(3);
        bottomCount.setColumns(3);
        final JPanel countPanel = new JPanel();
        countPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Retain only:", TitledBorder.LEFT, TitledBorder.TOP));
        countPanel.add(topLabel1);
        countPanel.add(topCount);
        countPanel.add(topLabel2);
        countPanel.add(bothLabel);
        countPanel.add(bottomLabel1);
        countPanel.add(bottomCount);
        countPanel.add(bottomLabel2);

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

        final JPanel acceptPanel = new JPanel();
        acceptPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Include classes:", TitledBorder.LEFT, TitledBorder.TOP));
        acceptPanel.setLayout(new BorderLayout());
        acceptPanel.add(accept, BorderLayout.CENTER);

        final JPanel rejectPanel = new JPanel();
        rejectPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Exclude classes:", TitledBorder.LEFT, TitledBorder.TOP));
        rejectPanel.setLayout(new BorderLayout());
        rejectPanel.add(reject, BorderLayout.CENTER);

        final JPanel jvmPanel = new JPanel();
        jvmPanel.add(dropJVM);

        localButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGraph();
            }
        });
        probeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateProbe();
            }
        });
        probeButton.setEnabled(false);
        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(localButton);
        buttonPanel.add(probeButton);
//        buttonPanel.add(generate);  TODO: popup window showing cmdline properties to make this filter the default

        addNorth(this, zeroPanel, countPanel, acceptPanel, rejectPanel, jvmPanel, buttonPanel);
    }

    public void setProbeManager(ProbeManager probe) {
        this.probe = probe;
        if(probe!=null) {
            probeButton.setEnabled(true);
        }
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
    private void addNorth(Container container, JComponent... components) {
        Container c = container;
        for(JComponent component : components) {
            c.setLayout(new BorderLayout());
            c.add(component, BorderLayout.NORTH);
            final JPanel innerContainer = new JPanel();
            c.add(innerContainer, BorderLayout.CENTER);
            c = innerContainer;
        }
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

    private void updateProbe() {
        if(probe!=null) {
            final StackFilter filter = getFilter();
            probe.getIOAccumulator().setFilter(filter);
        }
    }

    private void updateGraph() {
        if(graph!=null) {
            final StackFilter filter = getFilter();
            graph.setFilter(filter);
        }
    }
}
