package com.dell.gumshoe.tools.graph;

import com.dell.gumshoe.tools.graph.StackGraphPanel.IOStat;
import com.dell.gumshoe.tools.graph.StackGraphPanel.IOUnit;

/** configurable aspects of rendering frames and boxes */
public class DisplayOptions {
    public enum WidthScale { VALUE, LOG_VALUE, EQUAL }

    public enum Order { BY_VALUE, BY_NAME }

    final boolean byCalled; // true=flame graph; false=root graph
    final IOStat direction;
    final IOUnit value;
    final DisplayOptions.Order order;
    final DisplayOptions.WidthScale scale;
    final float minPercent;

    public DisplayOptions() {
        this(false, IOStat.READ, IOUnit.BYTES, DisplayOptions.Order.BY_NAME, DisplayOptions.WidthScale.VALUE, 0f);
    }

    public DisplayOptions(boolean byCalled, IOStat direction, IOUnit value, DisplayOptions.Order order, DisplayOptions.WidthScale scale, float minPercent) {
        this.byCalled = byCalled;
        this.direction = direction;
        this.value = value;
        this.order = order;
        this.scale = scale;
        this.minPercent = minPercent;
    }
}