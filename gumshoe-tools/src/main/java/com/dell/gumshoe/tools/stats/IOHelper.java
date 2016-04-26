package com.dell.gumshoe.tools.stats;

import static com.dell.gumshoe.tools.Swing.columns;
import static com.dell.gumshoe.tools.Swing.groupButtons;
import static com.dell.gumshoe.tools.Swing.stackNorth;

import com.dell.gumshoe.file.FileIODetailAdder;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.tools.graph.StackFrameNode;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import java.util.Map;
import java.util.Set;

public abstract class IOHelper extends DataTypeHelper {
    private final JRadioButton readStat = new JRadioButton("read", true);
    private final JRadioButton writeStat = new JRadioButton("write");
    private final JRadioButton bothStat = new JRadioButton("read+write");
    private final JRadioButton opsUnit = new JRadioButton("ops/count", true);
    private final JRadioButton bytesUnit = new JRadioButton("bytes");
    private final JRadioButton timeUnit = new JRadioButton("time(ms)");
    private final JRadioButton avgSize = new JRadioButton("bytes/op", true);
    private final JRadioButton rate = new JRadioButton("bytes/sec");

    protected abstract String getTargetName();

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
                                + "Target:\n%d %s: %s\n\n"
                                + "Traffic:\nRead: %d operations%s, %d bytes%s, %d ms %s\n"
                                + "Write: %d operations%s, %d bytes%s, %d ms %s\n"
                                + "Combined: %d operations%s, %d bytes%s, %d ms %s\n\n"
                                + "Calls %d methods: %s\n\n"
                                + "Called by %d methods: %s",
                                boxNode.getFrame(),
                                boxDetail.targets.size(), getTargetName(), boxDetail.targets.toString(),
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
        final JPanel unitPanel1 = columns(new JLabel("Measurement: "), opsUnit, bytesUnit, timeUnit);
        final JPanel unitPanel2 = columns(new JLabel(), avgSize, rate, new JLabel());

        return stackNorth(statPanel, unitPanel1, unitPanel2);
    }

    public enum IODirection { READ, WRITE, READ_PLUS_WRITE }

    private IODirection getDirection() {
        if(readStat.isSelected()) return IODirection.READ;
        else if(writeStat.isSelected()) return IODirection.WRITE;
        else if(bothStat.isSelected()) return IODirection.READ_PLUS_WRITE;
        else throw new IllegalStateException();
    }

    public enum IOUnit { OPS, BYTES, TIME, AVG_SIZE, RATE }

    private IOUnit getUnit() {
        if(opsUnit.isSelected()) return IOUnit.OPS;
        else if(bytesUnit.isSelected()) return IOUnit.BYTES;
        else if(timeUnit.isSelected()) return IOUnit.TIME;
        else if(avgSize.isSelected()) return IOUnit.AVG_SIZE;
        else if(rate.isSelected()) return IOUnit.RATE;
        else throw new IllegalStateException();
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
            default:                return getReadWriteValue(details);
        }
    }

    private long getReadWriteValue(IODetailAdder details) {
        switch(getUnit()) {
            case OPS:
            case BYTES:
            case TIME:  return getReadValue(details) + getWriteValue(details);
            case AVG_SIZE:
                final long totalBytes1 = getReadValue(details, IOUnit.BYTES)+getWriteValue(details, IOUnit.BYTES);
                final long totalOps = getReadValue(details, IOUnit.OPS)+getWriteValue(details, IOUnit.OPS);
                return totalBytes1/totalOps;
            case RATE:
                final long totalBytes2 = getReadValue(details, IOUnit.BYTES)+getWriteValue(details, IOUnit.BYTES);
                final long totalMillis = getReadValue(details, IOUnit.TIME)+getWriteValue(details, IOUnit.TIME);
                return totalBytes2/totalMillis;
        }
        throw new IllegalStateException();
    }

    private long getReadValue(IODetailAdder details) {
        return getReadValue(details, getUnit());
    }

    private long getReadValue(IODetailAdder details, IOUnit unit) {
        switch(unit) {
            case OPS:       return details.readCount.get();
            case BYTES:     return details.readBytes.get();
            case TIME:      return details.readTime.get();
            case AVG_SIZE:  return details.readBytes.get()/details.readCount.get();
            case RATE:      return details.readBytes.get()/details.readCount.get();
        }
        throw new IllegalStateException();
    }

    private long getWriteValue(IODetailAdder details) {
        return getWriteValue(details, getUnit());
    }

    private long getWriteValue(IODetailAdder details, IOUnit unit) {
        switch(unit) {
            case OPS:       return details.writeCount.get();
            case BYTES:     return details.writeBytes.get();
            case TIME:      return details.writeTime.get();
            case AVG_SIZE:  return details.writeBytes.get()/details.writeCount.get();
            case RATE:      return details.writeBytes.get()/details.writeCount.get();
        }
        throw new IllegalStateException();
    }
}
