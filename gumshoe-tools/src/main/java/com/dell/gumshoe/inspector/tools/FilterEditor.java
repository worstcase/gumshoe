package com.dell.gumshoe.inspector.tools;

import static com.dell.gumshoe.util.Swing.flow;
import static com.dell.gumshoe.util.Swing.groupButtons;
import static com.dell.gumshoe.util.Swing.rows;
import static com.dell.gumshoe.util.Swing.stackNorth;
import static com.dell.gumshoe.util.Swing.titled;

import com.dell.gumshoe.inspector.graph.StackGraphPanel;
import com.dell.gumshoe.stack.MinutiaFilter;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stack.StandardFilter;
import com.dell.gumshoe.stack.StandardFilter.Builder;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class FilterEditor extends JPanel implements HasCloseButton {
    private final JRadioButton revertZero = new JRadioButton("keep all frames");
    private final JRadioButton bucketZero = new JRadioButton("group as \"other\"", true);

    private final JTextField topCount = new JTextField();
    private final JTextField bottomCount = new JTextField();
    private final JLabel topLabel1 = new JLabel("top");
    private final JLabel topLabel2 = new JLabel("frames");
    private final JLabel bothLabel = new JLabel(" and ");
    private final JLabel bottomLabel1 = new JLabel("bottom");
    private final JLabel bottomLabel2 = new JLabel("frames");

    private final JCheckBox dropJVM = new JCheckBox("Drop jdk and gumshoe frames", true);

    // com.package.Class$Inner(Class:123)
    private final JRadioButton noSimplification = new JRadioButton("No simplification: com.package.Class$Inner.method(Class:123)", true);
    private final JRadioButton dropLine = new JRadioButton("Drop line numbers: com.package.Class$Inner.method(Class)");
    private final JRadioButton dropMethod = new JRadioButton("Drop method: com.package.Class$Inner(Class)");
    private final JRadioButton dropInner = new JRadioButton("Drop inner class: com.package.Class(Class)");
    private final JRadioButton dropClass = new JRadioButton("Drop class: com.package(Unknown)");

    private final JTextField recursionDepth = new JTextField();
    private final JTextField recursionThreshold = new JTextField();

    private final JTextArea accept = new JTextArea();
    private final JTextArea reject = new JTextArea();

    private final JButton localButton = new JButton("OK");
    private final JButton generate = new JButton("Generate cmdline options");

    private StackGraphPanel graph;

    public FilterEditor() {

        recursionDepth.setColumns(3);
        recursionDepth.setText("5");
        recursionThreshold.setColumns(3);
        recursionThreshold.setText("100");
        final JPanel recursionPanel = titled("Recursion filter:",
                flow(new JLabel("Remove repeated sequences up to "), recursionDepth,
                     new JLabel(" frames long when the stack has more than "), recursionThreshold,
                     new JLabel(" frames")));

        accept.setRows(3);
        reject.setRows(3);
        final JPanel acceptReject = stackNorth(
                new JLabel("Include only packages/classes:"), new JScrollPane(accept),
                new JLabel("Exclude packages/classes:"), new JScrollPane(reject));

        groupButtons(revertZero, bucketZero);
        final JPanel zeroPanel = flow(new JLabel("When stack filter matches no frames:"), revertZero, bucketZero);

        final JPanel chooseFrames = titled("Filter stack frames:",
                stackNorth(acceptReject,  flow(dropJVM), createCountPanel(), zeroPanel) );

        groupButtons(noSimplification, dropLine, dropMethod, dropInner, dropClass);
        final JPanel simplificationPanel = titled("Simplify stack frames:",
                rows(noSimplification, dropLine, dropMethod, dropInner, dropClass));


        localButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGraph();
            }
        });

        final JPanel contents =
                stackNorth(chooseFrames, recursionPanel, simplificationPanel, flow(localButton));

        setLayout(new BorderLayout());
        add(contents, BorderLayout.NORTH);
    }

    private JPanel createCountPanel() {
        topCount.setColumns(3);
        bottomCount.setColumns(3);

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

        return flow(new JLabel("Drop all frames except the"),
                topLabel1, topCount, bothLabel, bottomLabel1, bottomCount);
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
        final Builder builder = StandardFilter.builder();
        builder.withEndsOnly(getCount(topCount), getCount(bottomCount));
        if(dropJVM.isSelected()) { builder.withExcludePlatform(); }
        if(revertZero.isSelected()) { builder.withOriginalIfBlank(); }
        for(String acceptLine : getValues(accept)) { builder.withOnlyClasses(acceptLine); }
        for(String rejectLine : getValues(reject)) { builder.withExcludeClasses(rejectLine); }

        if(noSimplification.isSelected()) { builder.withSimpleFrames("NONE"); }
        else if(dropLine.isSelected()) { builder.withSimpleFrames(MinutiaFilter.Level.NO_LINE_NUMBERS); }
        else if(dropMethod.isSelected()) { builder.withSimpleFrames(MinutiaFilter.Level.NO_METHOD); }
        else if(dropInner.isSelected()) { builder.withSimpleFrames(MinutiaFilter.Level.NO_INNER_CLASSES); }
        else if(dropClass.isSelected()) { builder.withSimpleFrames(MinutiaFilter.Level.NO_CLASSES); }

        final int depth = getCount(recursionDepth);
        final int threshold = getCount(recursionThreshold);
        if(depth>0 && threshold>0) {
            builder.withRecursionFilter(depth, threshold);
        }

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

    @Override
    public void addCloseListener(ActionListener listener) {
        localButton.addActionListener(listener);
    }
}
