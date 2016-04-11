package com.dell.gumshoe.tools.graph;

import com.dell.gumshoe.socket.io.IODetailAccumulator;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.StatisticAccumulator;
import com.dell.gumshoe.tools.graph.StackGraphPanel.IOStat;
import com.dell.gumshoe.tools.graph.StackGraphPanel.IOUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** temporary intermediate model
 *
 *  stack with associated stats is broken into stack frames,
 *  these frames are linked in a tree structure and stats totaled per node.
 *
 *  the node and its position in the tree is later represented graphically by a Box
 */
public class StackFrameNode {
    long value;
    StackTraceElement frame;
    StatisticAccumulator detail;
    Map<StackTraceElement,StackFrameNode> divisions = new LinkedHashMap<>();
    StackFrameNode parent;
    boolean byCalled;

    private StackFrameNode() { }

    public StackFrameNode(Map<Stack, StatisticAccumulator> values, DisplayOptions options, StackFilter filter) {
        this.byCalled = options.byCalled;

        for(Map.Entry<Stack, StatisticAccumulator> entry : values.entrySet()) {
            final Stack origStack = entry.getKey();
            final StatisticAccumulator details = entry.getValue();
            final Stack displayStack = filter==null ? origStack : origStack.applyFilter(filter);
            final List<StackTraceElement> frames = Arrays.asList(displayStack.getFrames());

            // default is root graph -- group by top frame in stack performing the I/O.
            // root nodes will be rendered at the top of the image.
            //
            // for flame graph, reverse the stack frames to process entry point first [ie- Thread.run()]
            // child nodes in tree will be next frame up, the various things Thread.run() is calling.
            // the roots of this tree model are rendered at the bottom of the image in Box.draw().
            if(options.byCalled) { Collections.reverse(frames); }

            StackFrameNode node = this;
            if(node.detail==null) {
                node.detail = details.newInstance();
            }
            node.detail.add(details);
            final long nodeValue = getValue(details, options);
            node.value += nodeValue;
            for(StackTraceElement frame : frames) {
                StackFrameNode frameNode = node.divisions.get(frame);
                if(frameNode==null) {
                    frameNode = new StackFrameNode();
                    frameNode.parent = node;
                    frameNode.value = nodeValue;
                    frameNode.frame = frame;
                    frameNode.detail = details.newInstance();
                    node.divisions.put(frame, frameNode);
                } else {
                    frameNode.value += nodeValue;
                }
                node = frameNode;
                node.detail.add(details);
            }
        }
    }

    public StatisticAccumulator getDetail() { return detail; }
    public StackTraceElement getFrame() { return frame; }

    public Set<StackTraceElement> getCalledFrames() {
        return byCalled ? divisions.keySet() : getParentFrame();
    }

    public Set<StackTraceElement> getCallingFrames() {
        return byCalled ? getParentFrame() : divisions.keySet();
    }

    private Set<StackTraceElement> getParentFrame() {
        return parent.frame==null ? Collections.<StackTraceElement>emptySet() : Collections.<StackTraceElement>singleton(parent.frame);
    }

    private long getValue(StatisticAccumulator details, DisplayOptions options) {
        if( ! (details instanceof IODetailAccumulator)) { return 5; }
        final DisplayOptions.WidthScale width = options.scale;
        switch(width) {
            case EQUAL:     return 5;
            case VALUE:     return getStatValue((IODetailAccumulator) details, options);
            case LOG_VALUE:
            default:        return (long)(100. * Math.log1p(getStatValue((IODetailAccumulator) details, options)));
        }
    }

    private long getStatValue(IODetailAccumulator details, DisplayOptions options) {
        final IOStat stat = options.direction;
        final IOUnit value = options.value;
        switch(stat) {
            case READ:              return getReadValue(details, value);
            case WRITE:             return getWriteValue(details, value);
            case READ_PLUS_WRITE:
            default:                return getReadValue(details, value) + getWriteValue(details, value);
        }
    }

    private long getReadValue(IODetailAccumulator details, IOUnit unit) {
        switch(unit) {
            case OPS:   return details.readCount.get();
            case BYTES: return details.readBytes.get();
            case TIME:
            default:    return details.readTime.get();
        }
    }

    private long getWriteValue(IODetailAccumulator details, IOUnit unit) {
        switch(unit) {
            case OPS:   return details.writeCount.get();
            case BYTES: return details.writeBytes.get();
            case TIME:
            default:    return details.writeTime.get();
        }
    }

    public int getDepth(DisplayOptions options) {
        return getDepth(options, value*options.minPercent/100f);
    }
    public int getDepth(DisplayOptions options, float valueLimit) {
        final long value = getValue(detail, options);
        if(value<=valueLimit) { return 0; }
        int max = -1;
        for(StackFrameNode div : divisions.values()) {
            max = Math.max(div.getDepth(options, valueLimit), max);
        }
        return max + 1;
    }

    private List<StackTraceElement> getDivisions(final StackFrameNode model, DisplayOptions.Order order) {
        final List<StackTraceElement> keys = new ArrayList<>(model.divisions.keySet());
        if(order==DisplayOptions.Order.BY_VALUE) {
            Collections.sort(keys, new Comparator<StackTraceElement>() {
                @Override
                public int compare(StackTraceElement key1, StackTraceElement key2) {
                    return -Long.valueOf(model.divisions.get(key1).value).compareTo(Long.valueOf(model.divisions.get(key2).value));
                }
            });
        }
        return keys;
    }

    public List<Box> createBoxes(DisplayOptions options) {
        final List<Box> out = new ArrayList<Box>();
        final int depth = getDepth(options);
        Map<StackFrameNode,Long> priorRowModels = Collections.singletonMap(this, 0L);
        final float valueLimit = options.minPercent * value / 100f;
        for(int row = 0;row<depth;row++) {
            final Map<StackFrameNode,Long> thisRowModels = new LinkedHashMap<>();
            for(Map.Entry<StackFrameNode,Long> parentEntry : priorRowModels.entrySet()) {
                final long parentPosition = parentEntry.getValue();
                final StackFrameNode parent = parentEntry.getKey();
                final List<StackTraceElement> keys = getDivisions(parent, options.order);

                // first division left-aligned to parent position
                long position = parentPosition;
                for(StackTraceElement key : keys) {
                    final StackFrameNode divModel = parent.divisions.get(key);
                    if(divModel.value>valueLimit)  {
                        out.add(new Box(row, position, divModel, parent));
                        thisRowModels.put(divModel, position);
                        position += divModel.value;
                    }
                }
            }
            priorRowModels = thisRowModels;
        }
        return out;
    }

    @Override
    public String toString() {
        final StringBuilder msg = new StringBuilder();
        addNode(msg, "");
        return msg.toString();
    }

    private void addNode(StringBuilder builder, String indent) {
        for(Map.Entry<StackTraceElement,StackFrameNode> entry : divisions.entrySet()) {
            builder.append(indent)
                   .append(entry.getKey())
                   .append(" ")
                   .append(entry.getValue().detail)
                   .append("\n");
            entry.getValue().addNode(builder, indent + " ");
        }
    }
}