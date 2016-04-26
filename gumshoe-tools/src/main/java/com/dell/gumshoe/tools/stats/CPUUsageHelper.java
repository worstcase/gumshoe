package com.dell.gumshoe.tools.stats;

import static com.dell.gumshoe.tools.Swing.grid;
import static com.dell.gumshoe.tools.Swing.groupButtons;
import static com.dell.gumshoe.tools.Swing.rows;
import static com.dell.gumshoe.tools.Swing.stackNorth;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.stats.ValueReporter.Listener;
import com.dell.gumshoe.thread.CPUStats;
import com.dell.gumshoe.tools.graph.StackFrameNode;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import java.awt.BorderLayout;
import java.lang.Thread.State;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;

public class CPUUsageHelper extends DataTypeHelper {
    private final JRadioButton threadCountStat = new JRadioButton("all threads");
    private final JRadioButton runningThreadCountStat = new JRadioButton("running");
    private final JRadioButton waitingThreadCountStat = new JRadioButton("waiting");
    private final JRadioButton blockedThreadCountStat = new JRadioButton("blocked");
    private final JRadioButton timedWaitThreadCountStat = new JRadioButton("sleeping");
    private final JRadioButton cpuStat = new JRadioButton("CPU", true);
    private final JRadioButton cpuWeightedStat = new JRadioButton("CPU");
    private final JRadioButton userStat = new JRadioButton("user time");
    private final JRadioButton userWeightedStat = new JRadioButton("user");
    private final JRadioButton blockTimeStat = new JRadioButton("blocked");
    private final JRadioButton blockCountStat = new JRadioButton("blocked");
    private final JRadioButton blockCountByThreadsStat = new JRadioButton("blocked");
    private final JRadioButton blockTimeByThreadsStat = new JRadioButton("blocked");
    private final JRadioButton blockTimeByCountStat = new JRadioButton("blocked");
    private final JRadioButton waitTimeStat = new JRadioButton("waiting");
    private final JRadioButton waitCountStat = new JRadioButton("waiting");
    private final JRadioButton waitCountByThreadsStat = new JRadioButton("wait");
    private final JRadioButton waitTimeByThreadsStat = new JRadioButton("wait");
    private final JRadioButton waitTimeByCountStat = new JRadioButton("wait");
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
              boxDetail.getThreadCount(), getPercent(boxDetail.getThreadCount(), parentDetail.getThreadCount()),
              boxDetail.getThreadCount(State.RUNNABLE), boxDetail.getThreadCount(State.WAITING),
              boxDetail.getThreadCount(State.TIMED_WAITING), boxDetail.getThreadCount(State.BLOCKED),
              boxDetail.getCpuTime()/1000000f, boxDetail.getUserTime()/1000000f,
              boxDetail.getBlockedTime(), boxDetail.getWaitedTime() );
    }

    @Override
    public String getDetailText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final CPUStats boxDetail = (CPUStats)boxNode.getDetail();
        final CPUStats parentDetail = (CPUStats)parentNode.getDetail();
        final Set<StackTraceElement> callingFrames = boxNode.getCallingFrames();
        final Set<StackTraceElement> calledFrames = boxNode.getCalledFrames();
        return String.format("Frame: %s\n\n"

                + "%d threads%s: %d RUNNABLE%s, %d WAITING%s, %d TIMED_WAITING%s, %d BLOCKED%s\n\n"

                + "%.1f ms cpu time%s: %.1f ms user%s,"
                + " %d ms%s blocked (%d times%s),"
                + " %d ms%s waiting (%d times%s)\n\n"

                + "Calls %d methods: %s\n\n"
                + "Called by %d methods: %s",

              boxNode.getFrame(),

              boxDetail.getThreadCount(), getPercent(boxDetail.getThreadCount(), parentDetail.getThreadCount()),
              boxDetail.getThreadCount(State.RUNNABLE), getPercent(boxDetail.getThreadCount(State.RUNNABLE), parentDetail.getThreadCount(State.RUNNABLE)),
              boxDetail.getThreadCount(State.WAITING), getPercent(boxDetail.getThreadCount(State.WAITING), parentDetail.getThreadCount(State.WAITING)),
              boxDetail.getThreadCount(State.TIMED_WAITING), getPercent(boxDetail.getThreadCount(State.TIMED_WAITING), parentDetail.getThreadCount(State.TIMED_WAITING)),
              boxDetail.getThreadCount(State.BLOCKED), getPercent(boxDetail.getThreadCount(State.BLOCKED), parentDetail.getThreadCount(State.BLOCKED)),

              boxDetail.getCpuTime()/1000000f, getPercent(boxDetail.getCpuTime(), parentDetail.getCpuTime()),
              boxDetail.getUserTime()/1000000f, getPercent(boxDetail.getUserTime(), parentDetail.getUserTime()),
              boxDetail.getBlockedTime(), getPercent(boxDetail.getBlockedTime(), parentDetail.getBlockedTime()),
              boxDetail.getBlockedCount(), getPercent(boxDetail.getBlockedCount(), parentDetail.getBlockedCount()),
              boxDetail.getWaitedTime(), getPercent(boxDetail.getWaitedTime(), parentDetail.getWaitedTime()),
              boxDetail.getWaitedCount(), getPercent(boxDetail.getWaitedCount(), parentDetail.getWaitedCount()),

              calledFrames.size(), getFrames(calledFrames),
              callingFrames.size(), getFrames(callingFrames) );
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
                cpuStat, userStat, blockTimeStat, waitTimeStat, new JLabel(),
                blockCountStat, waitCountStat, new JLabel(), new JLabel(), new JLabel(),
                cpuWeightedStat, userWeightedStat, blockTimeByThreadsStat,  waitTimeByThreadsStat, new JLabel(),
                waitCountByThreadsStat, blockCountByThreadsStat, new JLabel(), new JLabel(), new JLabel(),
                waitTimeByCountStat,  blockTimeByCountStat );
        final JComponent labels = rows(
                new JLabel("Choose statistic value:"),
                new JLabel("Thread counts:"),
                new JLabel("Total times:"),
                new JLabel("Total event counts:"),
                new JLabel("Avg time per thread:"),
                new JLabel("Avg count per thread:"),
                new JLabel("Avg event duration:") );

        final JPanel out = new JPanel();
        out.setLayout(new BorderLayout());
        out.add(labels, BorderLayout.WEST);
        out.add(grid, BorderLayout.CENTER);

        final JPanel squishNorth = stackNorth(out);
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
