package com.dell.gumshoe.tools.stats;

import com.dell.gumshoe.socket.io.IODetailAccumulator;
import com.dell.gumshoe.socket.io.SocketIOAccumulator;
import com.dell.gumshoe.socket.unclosed.UnclosedStats;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAccumulator;
import com.dell.gumshoe.tools.graph.StackFrameNode;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

public class UnclosedSocketHelper extends DataTypeHelper {

    @Override
    public String getToolTipText(StackFrameNode boxNode, StackFrameNode parentNode) {
        final IODetailAccumulator boxDetail = (IODetailAccumulator)boxNode.getDetail();
        final IODetailAccumulator parentDetail = (IODetailAccumulator)parentNode.getDetail();
        return String.format("<html>\n"
                                + "%s<br>\n"
                                + "%d addresses<br>\n"
                                + "R %d ops%s %d bytes%s %d ms%s<br>\n"
                                + "W %d ops%s %d bytes%s %d ms%s<br>\n"
                                + "</html>",

                              boxNode.getFrame(),
                              boxDetail.addresses.size(),

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
        final IODetailAccumulator boxDetail = (IODetailAccumulator)boxNode.getDetail();
        final IODetailAccumulator parentDetail = (IODetailAccumulator)parentNode.getDetail();
        final Set<StackTraceElement> callingFrames = boxNode.getCallingFrames();
        final Set<StackTraceElement> calledFrames = boxNode.getCalledFrames();
        return String.format("Frame: %s\n\n"
                                + "Network:\n%d addresses: %s\n\n"
                                + "Traffic:\nRead: %d operations%s, %d bytes%s, %d ms %s\n"
                                + "Write: %d operations%s, %d bytes%s, %d ms %s\n\n"
                                + "Stack:\nCalls %d methods: %s"
                                + "\nCalled by %d methods: %s",
                                boxNode.getFrame(),
                                boxDetail.addresses.size(), boxDetail.addresses.toString(),
                                boxDetail.readCount.get(), getPercent(boxDetail.readCount, parentDetail.readCount),
                                boxDetail.readBytes.get(), getPercent(boxDetail.readBytes, parentDetail.readBytes),
                                boxDetail.readTime.get(), getPercent(boxDetail.readTime, parentDetail.readTime),
                                boxDetail.writeCount.get(), getPercent(boxDetail.writeCount, parentDetail.writeCount),
                                boxDetail.writeBytes.get(), getPercent(boxDetail.writeBytes, parentDetail.writeBytes),
                                boxDetail.writeTime.get(), getPercent(boxDetail.writeTime, parentDetail.writeTime),
                                calledFrames.size(), getFrames(calledFrames),
                                callingFrames.size(), getFrames(callingFrames) );
    }

    @Override
    public StatisticAccumulator parse(String value) throws ParseException {
        return IODetailAccumulator.fromString(value);
    }

    @Override
    public String getSummary(Map<Stack, StatisticAccumulator> data) {
        UnclosedStats tally = new UnclosedStats();
        for(StatisticAccumulator item : data.values()) {
            tally.add((UnclosedStats)item);
        }
        return data.size() + " entries, total " + tally;
    }


}
