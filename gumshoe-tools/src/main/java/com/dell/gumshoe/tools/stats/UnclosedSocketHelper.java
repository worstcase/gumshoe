package com.dell.gumshoe.tools.stats;

import static com.dell.gumshoe.tools.Swing.groupButtons;

import com.dell.gumshoe.socket.unclosed.UnclosedStats;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.tools.graph.StackFrameNode;
import static com.dell.gumshoe.tools.Swing.*;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

public class UnclosedSocketHelper extends DataTypeHelper {
    private final JRadioButton countStat = new JRadioButton("count", true);
    private final JRadioButton ageStat = new JRadioButton("age");

    @Override
    public String getToolTipText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final UnclosedStats boxDetail = (UnclosedStats)boxNode.getDetail();
        final UnclosedStats parentDetail = (UnclosedStats)parentNode.getDetail();
        return String.format("<html>\n"
                                + "%s<br>\n"
                                + "%d sockets%s opened up to %d ms ago\n"
                                + "</html>",

                              boxNode.getFrame(),
                              boxDetail.getCount(), getPercent(boxDetail.getCount(), parentDetail.getCount()),
                              boxDetail.getMaxAge() );
    }

    @Override
    public String getDetailText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final UnclosedStats boxDetail = (UnclosedStats)boxNode.getDetail();
        final UnclosedStats parentDetail = (UnclosedStats)parentNode.getDetail();
        final Set<StackTraceElement> callingFrames = boxNode.getCallingFrames();
        final Set<StackTraceElement> calledFrames = boxNode.getCalledFrames();
        return String.format("Frame: %s\n\n"
                                + "Count:\n%d open sockets%s\n\n"
                                + "Age:\nUp to %d ms\n\n"
                                + "Calls %d methods: %s\n\n"
                                + "Called by %d methods: %s",
                                boxNode.getFrame(),
                                boxDetail.getCount(), getPercent(boxDetail.getCount(), parentDetail.getCount()),
                                boxDetail.getMaxAge(),
                                calledFrames.size(), getFrames(calledFrames),
                                callingFrames.size(), getFrames(callingFrames) );
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
        return columns(new JLabel("Value: "), countStat, ageStat);
    }

    @Override
    protected long getStatValue(StatisticAdder composite) {
        final UnclosedStats value = (UnclosedStats) composite;
        return countStat.isSelected() ? value.getCount() : value.getMaxAge();
    }

}
