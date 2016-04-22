package com.dell.gumshoe.tools.stats;

import static com.dell.gumshoe.tools.Swing.columns;
import static com.dell.gumshoe.tools.Swing.groupButtons;
import static com.dell.gumshoe.tools.Swing.stackNorth;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.file.FileIODetailAdder;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.stats.ValueReporter;
import com.dell.gumshoe.stats.ValueReporter.Listener;
import com.dell.gumshoe.tools.graph.IODirection;
import com.dell.gumshoe.tools.graph.IOUnit;
import com.dell.gumshoe.tools.graph.StackFrameNode;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

public class FileIOHelper extends DataTypeHelper {
    private final JRadioButton readStat = new JRadioButton("read", true);
    private final JRadioButton writeStat = new JRadioButton("write");
    private final JRadioButton bothStat = new JRadioButton("read+write");
    private final JRadioButton opsUnit = new JRadioButton("ops/count", true);
    private final JRadioButton bytesUnit = new JRadioButton("bytes");
    private final JRadioButton timeUnit = new JRadioButton("time(ms)");

    private final JCheckBox acceptFileIO = new JCheckBox("file IO", true);

    @Override
    public JComponent getSelectionComponent() { return acceptFileIO; }

    @Override
    public boolean isSelected() { return acceptFileIO.isSelected(); }

    @Override
    public String getToolTipText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final IODetailAdder boxDetail = (IODetailAdder)boxNode.getDetail();
        final IODetailAdder parentDetail = (IODetailAdder)parentNode.getDetail();
        return String.format("<html>\n"
                                + "%s<br>\n"
                                + "%d files<br>\n"
                                + "R %d ops%s %d bytes%s %d ms%s<br>\n"
                                + "W %d ops%s %d bytes%s %d ms%s<br>\n"
                                + "R+W %d ops%s %d bytes%s %d ms%s<br>\n"
                                + "</html>",

                              boxNode.getFrame(),
                              boxDetail.targets.size(),

                              boxDetail.readCount.get(), getPercent(boxDetail.readCount, parentDetail.readCount),
                              boxDetail.readBytes.get(), getPercent(boxDetail.readBytes, parentDetail.readBytes),
                              boxDetail.readTime.get(), getPercent(boxDetail.readTime, parentDetail.readTime),

                              boxDetail.writeCount.get(), getPercent(boxDetail.writeCount, parentDetail.writeCount),
                              boxDetail.writeBytes.get(), getPercent(boxDetail.writeBytes, parentDetail.writeBytes),
                              boxDetail.writeTime.get(), getPercent(boxDetail.writeTime, parentDetail.writeTime),

                              boxDetail.writeCount.get() + boxDetail.readCount.get(),
                              getPercent(boxDetail.writeCount, boxDetail.readCount, parentDetail.writeCount, parentDetail.readCount),
                              boxDetail.writeBytes.get() + boxDetail.readBytes.get(),
                              getPercent(boxDetail.writeBytes, boxDetail.readBytes, parentDetail.writeBytes, parentDetail.writeBytes),
                              boxDetail.writeTime.get() + boxDetail.readTime.get(),
                              getPercent(boxDetail.writeTime, boxDetail.readTime, parentDetail.writeTime, parentDetail.readTime));
    }

    @Override
    public String getDetailText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final IODetailAdder boxDetail = (IODetailAdder)boxNode.getDetail();
        final IODetailAdder parentDetail = (IODetailAdder)parentNode.getDetail();
        final Set<StackTraceElement> callingFrames = boxNode.getCallingFrames();
        final Set<StackTraceElement> calledFrames = boxNode.getCalledFrames();
        return String.format("Frame: %s\n\n"
                                + "Network:\n%d files: %s\n\n"
                                + "Traffic:\nRead: %d operations%s, %d bytes%s, %d ms %s\n"
                                + "Write: %d operations%s, %d bytes%s, %d ms %s\n"
                                + "Combined: %d operations%s, %d bytes%s, %d ms %s\n\n"
                                + "Calls %d methods: %s\n\n"
                                + "Called by %d methods: %s",
                                boxNode.getFrame(),
                                boxDetail.targets.size(), boxDetail.targets.toString(),
                                boxDetail.readCount.get(), getPercent(boxDetail.readCount, parentDetail.readCount),
                                boxDetail.readBytes.get(), getPercent(boxDetail.readBytes, parentDetail.readBytes),
                                boxDetail.readTime.get(), getPercent(boxDetail.readTime, parentDetail.readTime),
                                boxDetail.writeCount.get(), getPercent(boxDetail.writeCount, parentDetail.writeCount),
                                boxDetail.writeBytes.get(), getPercent(boxDetail.writeBytes, parentDetail.writeBytes),
                                boxDetail.writeTime.get(), getPercent(boxDetail.writeTime, parentDetail.writeTime),
                                boxDetail.writeCount.get() + boxDetail.readCount.get(),
                                getPercent(boxDetail.writeCount, boxDetail.readCount, parentDetail.writeCount, parentDetail.readCount),
                                boxDetail.writeBytes.get() + boxDetail.readBytes.get(),
                                getPercent(boxDetail.writeBytes, boxDetail.readBytes, parentDetail.writeBytes, parentDetail.writeBytes),
                                boxDetail.writeTime.get() + boxDetail.readTime.get(),
                                getPercent(boxDetail.writeTime, boxDetail.readTime, parentDetail.writeTime, parentDetail.readTime),
                                calledFrames.size(), getFrames(calledFrames),
                                callingFrames.size(), getFrames(callingFrames) );
    }

    @Override
    public StatisticAdder parse(String value) throws ParseException {
        return FileIODetailAdder.fromString(value);
    }

    @Override
    public String getSummary(Map<Stack, StatisticAdder> data) {
        IODetailAdder tally = new FileIODetailAdder();
        for(StatisticAdder item : data.values()) {
            tally.add((IODetailAdder)item);
        }
        return data.size() + " entries, total " + tally;
    }

    @Override
    public JComponent getOptionEditor() {
        groupButtons(readStat, writeStat, bothStat);
//        final JPanel statPanel = stackWest(new JLabel("Operation:"), columns(readStat, writeStat, bothStat));
        final JPanel statPanel = columns(new JLabel("Operation: "), readStat, writeStat, bothStat);

        groupButtons(opsUnit, bytesUnit, timeUnit);
        final JPanel unitPanel = columns(new JLabel("Measurement: "), opsUnit, bytesUnit, timeUnit);

        return stackNorth(statPanel, unitPanel);
    }

    private IODirection getDirection() {
        if(readStat.isSelected()) return IODirection.READ;
        else if(writeStat.isSelected()) return IODirection.WRITE;
        else return IODirection.READ_PLUS_WRITE;
    }

    private IOUnit getUnit() {
        if(opsUnit.isSelected()) return IOUnit.OPS;
        else if(bytesUnit.isSelected()) return IOUnit.BYTES;
        else return IOUnit.TIME;
    }

    @Override
    public long getStatValue(StatisticAdder details) {
        return getValue((IODetailAdder)details);
    }

    private long getValue(IODetailAdder details) {
        switch(getDirection()) {
            case READ:              return getReadValue(details);
            case WRITE:             return getWriteValue(details);
            case READ_PLUS_WRITE:
            default:                return getReadValue(details) + getWriteValue(details);
        }
    }

    private long getReadValue(IODetailAdder details) {
        switch(getUnit()) {
            case OPS:   return details.readCount.get();
            case BYTES: return details.readBytes.get();
            case TIME:
            default:    return details.readTime.get();
        }
    }

    private long getWriteValue(IODetailAdder details) {
        switch(getUnit()) {
            case OPS:   return details.writeCount.get();
            case BYTES: return details.writeBytes.get();
            case TIME:
            default:    return details.writeTime.get();
        }
    }

    @Override
    public void addListener(ProbeManager probe, Listener listener) {
        final ValueReporter<IODetailAdder> reporter = probe.getFileIOReporter();
        if(reporter!=null) {
            reporter.addListener(listener);
        }
    }
}
