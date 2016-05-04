package com.dell.gumshoe.inspector.graph;

import com.dell.gumshoe.inspector.helper.DataTypeHelper;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.StatisticAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** temporary intermediate model
 *
 *  stack with associated stats is broken into stack frames,
 *  these frames are linked in a tree structure and stats totaled per node.
 *
 *  the node and its position in the tree is later represented graphically by a Box
 */
public class StackFrameNode {
    private long value;
    private StatisticAdder detail;
    private Map<StackTraceElement,StackFrameNode> divisions = new LinkedHashMap<>();
    private StackFrameNode parent;
    private boolean byCalled;
    private StackTraceElement[] frames;
    private int frameIndex;

    private StackFrameNode(boolean byCalled, StackFrameNode node, long nodeValue, StackTraceElement[] frames, int frameIndex, StatisticAdder detail) {
        this.byCalled = byCalled;
        this.parent = node;
        this.value = nodeValue;
        this.frames = frames;
        this.frameIndex = frameIndex;
        this.detail = detail;
    }

    public StackFrameNode(Map<Stack, StatisticAdder> values, DisplayOptions options, StackFilter filter) {
        this.byCalled = options.byCalled;
        this.frames = null;

        for(Map.Entry<Stack, StatisticAdder> entry : values.entrySet()) {
            final Stack origStack = entry.getKey();
            final StatisticAdder details = entry.getValue();
            final Stack displayStack = filter==null ? origStack : origStack.applyFilter(filter);
            final StackTraceElement[] frames = displayStack.getFrames();

            // default is root graph -- group by top frame in stack performing the I/O.
            // root nodes will be rendered at the top of the image.
            //
            // for flame graph, reverse the stack frames to process entry point first [ie- Thread.run()]
            // child nodes in tree will be next frame up, the various things Thread.run() is calling.
            // the roots of this tree model are rendered at the bottom of the image in Box.draw().
            if(options.byCalled) { Collections.reverse(Arrays.asList(frames)); }

            StackFrameNode node = this;
            if(node.detail==null) {
                node.detail = details.newInstance();
            }
            node.detail.add(details);
            final long nodeValue = getValue(details, options);
            node.value += nodeValue;
            for(int frameIndex=0;frameIndex<frames.length;frameIndex++) {
                StackTraceElement frame = frames[frameIndex];
                StackFrameNode frameNode = node.divisions.get(frame);
                if(frameNode==null) {
                    frameNode = new StackFrameNode(byCalled, node, nodeValue, frames, frameIndex, details.newInstance());
                    node.divisions.put(frame, frameNode);
                } else {
                    frameNode.value += nodeValue;
                }
                node = frameNode;
                node.detail.add(details);
            }
        }
    }

    public StatisticAdder getDetail() { return detail; }
    public StackTraceElement getFrame() { return frames==null ? null : frames[frameIndex]; }
    public long getValue() { return value; }

    public StackTraceElement[] getContext() {
        final StackTraceElement[] out = new StackTraceElement[frameIndex+1];
        int i=0;
        if(byCalled) {
            for(int index=frameIndex;index>=0;index--) {
                out[i++] = frames[index];
            }
        } else {
            for(int index=0;index<=frameIndex;index++) {
                out[i++] = frames[index];
            }
        }
        return out;
    }

    public void appendContext(StringBuilder msg) {
        if(byCalled) {
            for(int index=frameIndex;index>=0;index--) {
                msg.append(frames[index]).append("\n");
            }
        } else {
            for(int index=0;index<=frameIndex;index++) {
                msg.append(frames[index]).append("\n");
            }
        }
    }

    public Set<StackTraceElement> getCalledFrames() {
        return byCalled ? divisions.keySet() : getParentFrame();
    }

    public Set<StackTraceElement> getCallingFrames() {
        return byCalled ? getParentFrame() : divisions.keySet();
    }

    public boolean isByCalled() {
        return byCalled;
    }

    private Set<StackTraceElement> getParentFrame() {
        final StackTraceElement parentFrame = parent.getFrame();
        return parentFrame==null ? Collections.<StackTraceElement>emptySet() : Collections.<StackTraceElement>singleton(parentFrame);    }

    private long getValue(StatisticAdder details, DisplayOptions options) {
        final DisplayOptions.WidthScale width = options.scale;
        switch(width) {
            case EQUAL:     return 5;
            case VALUE:     return getStatValue(details, options);
            case LOG_VALUE: return (long)(100. * Math.log1p(getStatValue(details, options)));
        }
        throw new IllegalArgumentException("unknown width scale: " + width);
    }

    private long getStatValue(StatisticAdder details, DisplayOptions options) {
        return DataTypeHelper.getValue(details);
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

    /////


    /* convert this tree of all nodes into list of boxes for display */
    public List<Box> createBoxes(DisplayOptions options) {
        final List<Box> out = new ArrayList<>();
        final int depth = getDepth(options);

        final Box rootBox = new Box(1, 0f, 1f, this, null);
        Map<StackFrameNode,Box> priorRowModels = Collections.singletonMap(this, rootBox);

        // display option: do not show box if value <= minPercent
        final float valueLimit = options.minPercent * value / 100f;

        for(int row = 0;row<depth;row++) {
            final Map<StackFrameNode,Box> thisRowModels = new LinkedHashMap<>();

            for(Map.Entry<StackFrameNode,Box> parentEntry : priorRowModels.entrySet()) {
                final Box parentBox = parentEntry.getValue();
                final float parentPosition = parentBox.getPosition();
                final StackFrameNode parent = parentEntry.getKey();

                final List<StackTraceElement> keys = getDivisions(parent, options.order);

                // first division is left-aligned with parent position
                float position = parentPosition;

                // each division width is a portion of parent width
                long childValuesSum = 0;
                for(StackTraceElement key : keys) {
                    final StackFrameNode divModel = parent.divisions.get(key);
                    childValuesSum += divModel.value;
                }
                final float scaleValue = childValuesSum/parentBox.getWidth();

                for(StackTraceElement key : keys) {
                    final StackFrameNode divModel = parent.divisions.get(key);
                    if(divModel.value>valueLimit)  {
                        final float relativeValue = (divModel.value) / scaleValue;
                        final Box box = new Box(row, position, relativeValue, divModel, parent);
                        out.add(box);
                        thisRowModels.put(divModel, box);
                        position += relativeValue;
                    }
                }
            }

            priorRowModels = thisRowModels;
        }
        return out;
    }

    /////

    @Override
    public String toString() {
        final StringBuilder msg = new StringBuilder();
        addNode(msg, "");
        return msg.toString();
    }

    private void addNode(StringBuilder builder, String indent) {
        for(Map.Entry<StackTraceElement,StackFrameNode> entry : divisions.entrySet()) {
            builder.append(indent)
                   .append(entry.getValue().value)
                   .append(" ")
                   .append(entry.getKey())
                   .append(" ")
                   .append(entry.getValue().detail)
                   .append("\n");
            entry.getValue().addNode(builder, indent + " ");
        }
    }

    public String getStats(StackFrameNode parent) {
        return DataTypeHelper.getStatInfo(detail, parent==null ? null : parent.detail);
    }
}