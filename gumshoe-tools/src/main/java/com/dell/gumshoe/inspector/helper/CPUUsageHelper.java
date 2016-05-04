package com.dell.gumshoe.inspector.helper;

import static com.dell.gumshoe.util.Swing.grid;
import static com.dell.gumshoe.util.Swing.groupButtons;
import static com.dell.gumshoe.util.Swing.rows;
import static com.dell.gumshoe.util.Swing.stackNorth;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.inspector.graph.StackFrameNode;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.stats.ValueReporter.Listener;
import com.dell.gumshoe.thread.CPUStats;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import java.awt.BorderLayout;
import java.lang.Thread.State;
import java.text.ParseException;
import java.util.Map;

public class CPUUsageHelper extends DataTypeHelper {
    private final JRadioButton threadCountStat = new JRadioButton("all threads");
    private final JRadioButton runningThreadCountStat = new JRadioButton("runnable");
    private final JRadioButton waitingThreadCountStat = new JRadioButton("waiting");
    private final JRadioButton blockedThreadCountStat = new JRadioButton("blocked");
    private final JRadioButton timedWaitThreadCountStat = new JRadioButton("sleeping");
    private final JRadioButton cpuStat = new JRadioButton("CPU", true);
    private final JRadioButton cpuWeightedStat = new JRadioButton("CPU **");
    private final JRadioButton userStat = new JRadioButton("user");
    private final JRadioButton userWeightedStat = new JRadioButton("user **");
    private final JRadioButton blockTimeStat = new JRadioButton("blocked");
    private final JRadioButton blockCountStat = new JRadioButton("blocked");
    private final JRadioButton blockCountByThreadsStat = new JRadioButton("blocked **");
    private final JRadioButton blockTimeByThreadsStat = new JRadioButton("blocked **");
    private final JRadioButton blockTimeByCountStat = new JRadioButton("blocked **");
    private final JRadioButton waitTimeStat = new JRadioButton("waiting");
    private final JRadioButton waitCountStat = new JRadioButton("waiting");
    private final JRadioButton waitCountByThreadsStat = new JRadioButton("waiting **");
    private final JRadioButton waitTimeByThreadsStat = new JRadioButton("waiting **");
    private final JRadioButton waitTimeByCountStat = new JRadioButton("waiting **");
    private final JCheckBox acceptCPUStats = new JCheckBox("CPU usage", true);

    @Override
    public JComponent getSelectionComponent() { return acceptCPUStats; }

    @Override
    public boolean isSelected() { return acceptCPUStats.isSelected(); }

    @Override
    public String getToolTipText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final CPUStats boxDetail = (CPUStats)boxNode.getDetail();
        final CPUStats parentDetail = (CPUStats)parentNode.getDetail();
        return String.format("<html>\n"
                + "%s<br>\n"
                + "%d threads(%s) = %d R + %d W + %d T + %d B<br>\n"
                + "%.1f ms = %.1f u + %d b + %d w\n"
                + "</html>",

              boxNode.getFrame(),
              boxDetail.getThreadCount(), pct(boxDetail.getThreadCount(), parentDetail.getThreadCount()),
              boxDetail.getThreadCount(State.RUNNABLE), boxDetail.getThreadCount(State.WAITING),
              boxDetail.getThreadCount(State.TIMED_WAITING), boxDetail.getThreadCount(State.BLOCKED),
              boxDetail.getCpuTime()/1000000f, boxDetail.getUserTime()/1000000f,
              boxDetail.getBlockedTime(), boxDetail.getWaitedTime() );
    }

    @Override
    public String getStatDetails(StatisticAdder nodeValue, StatisticAdder parentValue) {
        final CPUStats boxDetail = (CPUStats)nodeValue;
        final CPUStats parentDetail = (CPUStats)parentValue;

        return String.format(
                "%d threads%s: %d RUNNABLE%s, %d WAITING%s, %d TIMED_WAITING%s, %d BLOCKED%s\n\n"

                + "%.1f ms cpu time%s: %.1f ms user%s,"
                + " %d ms%s blocked (%d times%s),"
                + " %d ms%s waiting (%d times%s)\n\n",

              boxDetail.getThreadCount(), pct(boxDetail.getThreadCount(), parentDetail.getThreadCount()),
              boxDetail.getThreadCount(State.RUNNABLE), pct(boxDetail.getThreadCount(State.RUNNABLE), parentDetail.getThreadCount(State.RUNNABLE)),
              boxDetail.getThreadCount(State.WAITING), pct(boxDetail.getThreadCount(State.WAITING), parentDetail.getThreadCount(State.WAITING)),
              boxDetail.getThreadCount(State.TIMED_WAITING), pct(boxDetail.getThreadCount(State.TIMED_WAITING), parentDetail.getThreadCount(State.TIMED_WAITING)),
              boxDetail.getThreadCount(State.BLOCKED), pct(boxDetail.getThreadCount(State.BLOCKED), parentDetail.getThreadCount(State.BLOCKED)),

              boxDetail.getCpuTime()/1000000f, pct(boxDetail.getCpuTime(), parentDetail.getCpuTime()),
              boxDetail.getUserTime()/1000000f, pct(boxDetail.getUserTime(), parentDetail.getUserTime()),
              boxDetail.getBlockedTime(), pct(boxDetail.getBlockedTime(), parentDetail.getBlockedTime()),
              boxDetail.getBlockedCount(), pct(boxDetail.getBlockedCount(), parentDetail.getBlockedCount()),
              boxDetail.getWaitedTime(), pct(boxDetail.getWaitedTime(), parentDetail.getWaitedTime()),
              boxDetail.getWaitedCount(), pct(boxDetail.getWaitedCount(), parentDetail.getWaitedCount()));
    }

    @Override
    public StatisticAdder parse(String value) throws ParseException {
        return CPUStats.fromString(value);
    }

    @Override
    public String getSummary(Map<Stack, StatisticAdder> data) {
        CPUStats tally = new CPUStats();
        for(StatisticAdder item : data.values()) {
            tally.add(item);
        }
        return data.size() + " entries, total " + tally;
    }

    @Override
    public JComponent getOptionEditor() {
        groupButtons( threadCountStat,
                runningThreadCountStat, waitingThreadCountStat, blockedThreadCountStat, timedWaitThreadCountStat,
                cpuStat, cpuWeightedStat, userStat, userWeightedStat,
                blockCountStat, blockTimeStat, blockCountByThreadsStat, blockTimeByThreadsStat, blockTimeByCountStat,
                waitCountStat, waitTimeStat, waitCountByThreadsStat, waitTimeByThreadsStat, waitTimeByCountStat);

        final JComponent grid = grid(5,
                new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(),
                threadCountStat, runningThreadCountStat, waitingThreadCountStat, blockedThreadCountStat, timedWaitThreadCountStat,
                cpuStat, userStat, waitTimeStat, blockTimeStat, new JLabel(),
                new JLabel(), new JLabel(), waitCountStat, blockCountStat, new JLabel(),
                cpuWeightedStat, userWeightedStat, waitTimeByThreadsStat, blockTimeByThreadsStat,  new JLabel(),
                new JLabel(), new JLabel(), waitCountByThreadsStat, blockCountByThreadsStat, new JLabel(),
                new JLabel(), new JLabel(), waitTimeByCountStat,  blockTimeByCountStat );
        final JComponent labels = rows(
                new JLabel("Choose statistic value:"),
                new JLabel("Thread counts:"),
                new JLabel("Total times:"),
                new JLabel("Total event counts:"),
                new JLabel("Avg time per thread:"),
                new JLabel("Avg count per thread:"),
                new JLabel("Avg event duration:") );

        final JPanel statChoices = new JPanel();
        statChoices.setLayout(new BorderLayout());
        statChoices.add(labels, BorderLayout.WEST);
        statChoices.add(grid, BorderLayout.CENTER);

        final JPanel squishNorth = stackNorth(statChoices, getDisclaimer());
        return squishNorth;
    }

    @Override
    public long getStatValue(StatisticAdder composite) {
        final CPUStats value = (CPUStats) composite;
        if(threadCountStat.isSelected()) { return value.getThreadCount(); }
        if(runningThreadCountStat.isSelected()) { return value.getThreadCount(State.RUNNABLE); }
        if(waitingThreadCountStat.isSelected()) { return value.getThreadCount(State.WAITING); }
        if(blockedThreadCountStat.isSelected()) { return value.getThreadCount(State.BLOCKED); }
        if(timedWaitThreadCountStat.isSelected()) { return value.getThreadCount(State.TIMED_WAITING); }
        if(cpuStat.isSelected()) { return value.getCpuTime(); }
        if(cpuWeightedStat.isSelected()) { return value.getCpuTime()/value.getThreadCount(); }
        if(userStat.isSelected()) { return value.getUserTime(); }
        if(userWeightedStat.isSelected()) { return value.getUserTime()/value.getThreadCount(); }
        if(blockCountStat.isSelected()) { return value.getBlockedCount(); }
        if(blockTimeStat.isSelected()) { return value.getBlockedTime(); }
        if(blockCountByThreadsStat.isSelected()) { return value.getBlockedCount()/value.getThreadCount(); }
        if(blockTimeByThreadsStat.isSelected()) { return value.getBlockedTime()/value.getThreadCount(); }
        if(blockTimeByCountStat.isSelected()) { return value.getBlockedTime()/value.getBlockedCount(); }
        if(waitCountStat.isSelected()) { return value.getWaitedCount(); }
        if(waitTimeStat.isSelected()) { return value.getWaitedTime(); }
        if(waitCountByThreadsStat.isSelected()) { return value.getWaitedCount()/value.getThreadCount(); }
        if(waitTimeByThreadsStat.isSelected()) { return value.getWaitedTime()/value.getThreadCount(); }
        if(waitTimeByCountStat.isSelected()) { return value.getWaitedTime()/value.getWaitedCount(); }
        else throw new IllegalStateException("no stat selected");
    }

    @Override
    public void addListener(ProbeManager probe, Listener listener) {
        final ValueReporter<CPUStats> reporter = probe.getCPUReporter();
        if(reporter!=null) {
            reporter.addListener(listener);
        }
    }

}
