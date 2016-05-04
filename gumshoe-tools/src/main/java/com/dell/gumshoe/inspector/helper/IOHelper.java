package com.dell.gumshoe.inspector.helper;

import static com.dell.gumshoe.util.Swing.columns;
import static com.dell.gumshoe.util.Swing.groupButtons;
import static com.dell.gumshoe.util.Swing.stackNorth;

import com.dell.gumshoe.file.FileIODetailAdder;
import com.dell.gumshoe.inspector.graph.StackFrameNode;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.StatisticAdder;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import java.util.Map;

public abstract class IOHelper extends DataTypeHelper {
    private final JRadioButton readStat = new JRadioButton("read", true);
    private final JRadioButton writeStat = new JRadioButton("write");
    private final JRadioButton bothStat = new JRadioButton("read+write");
    private final JRadioButton opsUnit = new JRadioButton("ops/count", true);
    private final JRadioButton bytesUnit = new JRadioButton("bytes");
    private final JRadioButton timeUnit = new JRadioButton("time(ms)");
    private final JRadioButton avgSize = new JRadioButton("bytes/op **", true);
    private final JRadioButton avgTime = new JRadioButton("ms/op **", true);
    private final JRadioButton rate = new JRadioButton("bytes/sec **");

    protected abstract String getTargetName();

    @Override
    public String getToolTipText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final IODetailAdder boxDetail = (IODetailAdder)boxNode.getDetail();
        final IODetailAdder parentDetail = (IODetailAdder)parentNode.getDetail();
        return String.format("<html>\n"
                                + "%s = %d%s<br>\n"
                                + "%d %s<br>\n"
                                + "R %d ops%s %d bytes%s %d ms%s<br>\n"
                                + "W %d ops%s %d bytes%s %d ms%s<br>\n"
                                + "</html>",

                              boxNode.getFrame(), getValue(boxDetail), pct(getValue(boxDetail), getValue(parentDetail)),
                              boxDetail.targets.size(), getTargetName(),

                              boxDetail.readCount.get(), pct(boxDetail.readCount, parentDetail.readCount),
                              boxDetail.readBytes.get(), pct(boxDetail.readBytes, parentDetail.readBytes),
                              boxDetail.readTime.get(), pct(boxDetail.readTime, parentDetail.readTime),

                              boxDetail.writeCount.get(), pct(boxDetail.writeCount, parentDetail.writeCount),
                              boxDetail.writeBytes.get(), pct(boxDetail.writeBytes, parentDetail.writeBytes),
                              boxDetail.writeTime.get(), pct(boxDetail.writeTime, parentDetail.writeTime),

                              boxDetail.writeCount.get() + boxDetail.readCount.get(),
                              pct(boxDetail.writeCount, boxDetail.readCount, parentDetail.writeCount, parentDetail.readCount),
                              boxDetail.writeBytes.get() + boxDetail.readBytes.get(),
                              pct(boxDetail.writeBytes, boxDetail.readBytes, parentDetail.writeBytes, parentDetail.writeBytes),
                              boxDetail.writeTime.get() + boxDetail.readTime.get(),
                              pct(boxDetail.writeTime, boxDetail.readTime, parentDetail.writeTime, parentDetail.readTime));
    }

    @Override
    public String getStatDetails(StatisticAdder nodeValue, StatisticAdder parentValue) {
        final IODetailAdder boxDetail = (IODetailAdder)nodeValue;
        final IODetailAdder parentDetail = (IODetailAdder)parentValue;
        return String.format("Target:\n%d %s: %s\n\n"
                              + "Read:\n"
                              + "%d operations%s, %d bytes%s, %d ms %s\n"
                              + "%d bytes/op, %d ms/op, %d bytes/ms\n\n"
                              + "Write:\n"
                              + "%d operations%s, %d bytes%s, %d ms %s\n"
                              + "%d bytes/op, %d ms/op, %d bytes/ms\n\n"
                              + "Combined:\n"
                              + "%d operations%s, %d bytes%s, %d ms %s\n"
                              + "%d bytes/op, %d ms/op, %d bytes/ms\n\n",

                              boxDetail.targets.size(), getTargetName(), boxDetail.targets.toString(),

                              boxDetail.readCount.get(), pct(boxDetail.readCount, parentDetail.readCount),
                              boxDetail.readBytes.get(), pct(boxDetail.readBytes, parentDetail.readBytes),
                              boxDetail.readTime.get(), pct(boxDetail.readTime, parentDetail.readTime),
                              getReadValue(boxDetail, IOUnit.AVG_SIZE), getReadValue(boxDetail, IOUnit.AVG_TIME), getReadValue(boxDetail, IOUnit.RATE),

                              boxDetail.writeCount.get(), pct(boxDetail.writeCount, parentDetail.writeCount),
                              boxDetail.writeBytes.get(), pct(boxDetail.writeBytes, parentDetail.writeBytes),
                              boxDetail.writeTime.get(), pct(boxDetail.writeTime, parentDetail.writeTime),
                              getWriteValue(boxDetail, IOUnit.AVG_SIZE), getWriteValue(boxDetail, IOUnit.AVG_TIME), getWriteValue(boxDetail, IOUnit.RATE),

                              getReadWriteValue(boxDetail, IOUnit.OPS),
                              pct(boxDetail.writeCount, boxDetail.readCount, parentDetail.writeCount, parentDetail.readCount),
                              getReadWriteValue(boxDetail, IOUnit.BYTES),
                              pct(boxDetail.writeBytes, boxDetail.readBytes, parentDetail.writeBytes, parentDetail.writeBytes),
                              getReadWriteValue(boxDetail, IOUnit.TIME),
                              pct(boxDetail.writeTime, boxDetail.readTime, parentDetail.writeTime, parentDetail.readTime),
                              getReadWriteValue(boxDetail, IOUnit.AVG_SIZE),
                              getReadWriteValue(boxDetail, IOUnit.AVG_TIME),
                              getReadWriteValue(boxDetail, IOUnit.RATE));
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
        final JPanel statPanel = columns(new JLabel("Operation: "), readStat, writeStat, bothStat);

        groupButtons(opsUnit, bytesUnit, timeUnit, avgSize, avgTime, rate);
        final JPanel unitPanel1 = columns(new JLabel("Measurement: "), opsUnit, bytesUnit, timeUnit);
        final JPanel unitPanel2 = columns(new JLabel(), avgSize, avgTime, rate);
        return stackNorth(statPanel, unitPanel1, unitPanel2, getDisclaimer());
    }

    public enum IODirection { READ, WRITE, READ_PLUS_WRITE }

    private IODirection getDirection() {
        if(readStat.isSelected()) return IODirection.READ;
        else if(writeStat.isSelected()) return IODirection.WRITE;
        else if(bothStat.isSelected()) return IODirection.READ_PLUS_WRITE;
        else throw new IllegalStateException();
    }

    public enum IOUnit { OPS, BYTES, TIME, AVG_SIZE, AVG_TIME, RATE }

    private IOUnit getUnit() {
        if(opsUnit.isSelected()) return IOUnit.OPS;
        else if(bytesUnit.isSelected()) return IOUnit.BYTES;
        else if(timeUnit.isSelected()) return IOUnit.TIME;
        else if(avgSize.isSelected()) return IOUnit.AVG_SIZE;
        else if(avgTime.isSelected()) return IOUnit.AVG_TIME;
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
        return getReadWriteValue(details, getUnit());
    }
    private long getReadWriteValue(IODetailAdder details, IOUnit unit) {
        switch(unit) {
            case OPS:
            case BYTES:
            case TIME:  return getReadValue(details) + getWriteValue(details);
            case AVG_SIZE:
                final long totalBytes1 = getReadValue(details, IOUnit.BYTES)+getWriteValue(details, IOUnit.BYTES);
                final long totalOps1 = getReadValue(details, IOUnit.OPS)+getWriteValue(details, IOUnit.OPS);
                return div(totalBytes1, totalOps1);
            case AVG_TIME:
                final long totalMillis1 = getReadValue(details, IOUnit.TIME)+getWriteValue(details, IOUnit.TIME);
                final long totalOps2 = getReadValue(details, IOUnit.OPS)+getWriteValue(details, IOUnit.OPS);
                return div(totalMillis1, totalOps2);
            case RATE:
                final long totalBytes2 = getReadValue(details, IOUnit.BYTES)+getWriteValue(details, IOUnit.BYTES);
                final long totalMillis2 = getReadValue(details, IOUnit.TIME)+getWriteValue(details, IOUnit.TIME);
                return div(totalBytes2, totalMillis2);
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
            case AVG_SIZE:  return div(details.readBytes, details.readCount);
            case AVG_TIME:  return div(details.readTime, details.readCount);
            case RATE:      return div(details.readBytes, details.readTime);
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
            case AVG_SIZE:  return div(details.writeBytes, details.writeCount);
            case AVG_TIME:  return div(details.writeTime, details.writeCount);
            case RATE:      return div(details.writeBytes, details.writeTime);
        }
        throw new IllegalStateException();
    }
}
