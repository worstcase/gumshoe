package com.dell.gumshoe.inspector.helper;

import static com.dell.gumshoe.util.Swing.flow;
import static com.dell.gumshoe.util.Swing.groupButtons;
import static com.dell.gumshoe.util.Swing.stackNorth;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.inspector.graph.StackFrameNode;
import com.dell.gumshoe.network.UnclosedStats;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.stats.ValueReporter.Listener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import java.text.ParseException;
import java.util.Map;

public class UnclosedSocketHelper extends DataTypeHelper {
    private final JRadioButton countStat = new JRadioButton("count", true);
    private final JRadioButton ageStat = new JRadioButton("age **");
    private final JCheckBox acceptSocketUnclosed = new JCheckBox("unclosed sockets");

    @Override
    public JComponent getSelectionComponent() { return acceptSocketUnclosed; }

    @Override
    public boolean isSelected() { return acceptSocketUnclosed.isSelected(); }

    @Override
    public String getToolTipText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final UnclosedStats boxDetail = (UnclosedStats)boxNode.getDetail();
        final UnclosedStats parentDetail = (UnclosedStats)parentNode.getDetail();
        return String.format("<html>\n"
                                + "%s<br>\n"
                                + "%d sockets%s opened up to %d ms ago\n"
                                + "</html>",

                              boxNode.getFrame(),
                              boxDetail.getCount(), pct(boxDetail.getCount(), parentDetail.getCount()),
                              boxDetail.getMaxAge() );
    }

    @Override
    public String getStatDetails(StatisticAdder nodeValue, StatisticAdder parentValue) {
        final UnclosedStats boxDetail = (UnclosedStats)nodeValue;
        final UnclosedStats parentDetail = (UnclosedStats)parentValue;
        return String.format(
                "Count:\n"
                + "%d open sockets%s\n\n"
                + "Age:\n"
                + "Up to %d ms\n\n",
                boxDetail.getCount(), pct(boxDetail.getCount(), parentDetail.getCount()),
                boxDetail.getMaxAge() );
    }

    @Override
    public StatisticAdder parse(String value) throws ParseException {
        return UnclosedStats.fromString(value);
    }

    @Override
    public String getSummary(Map<Stack, StatisticAdder> data) {
        UnclosedStats tally = new UnclosedStats();
        for(StatisticAdder item : data.values()) {
            tally.add((UnclosedStats)item);
        }
        return data.size() + " entries, total " + tally;
    }

    @Override
    public JComponent getOptionEditor() {
        groupButtons(countStat, ageStat);
        return stackNorth(flow(new JLabel("Value: "), countStat, ageStat), getDisclaimer());
    }

    @Override
    public long getStatValue(StatisticAdder composite) {
        final UnclosedStats value = (UnclosedStats) composite;
        return countStat.isSelected() ? value.getCount() : value.getMaxAge();
    }

    @Override
    public void addListener(ProbeManager probe, Listener listener) {
        final ValueReporter<UnclosedStats> reporter = probe.getUnclosedReporter();
        if(reporter!=null) {
            reporter.addListener(listener);
        }
    }

}
