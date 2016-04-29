package com.dell.gumshoe.inspector.graph;


/** configurable aspects of rendering frames and boxes */
public class DisplayOptions {
    public enum WidthScale { VALUE, LOG_VALUE, EQUAL }
    public enum Order { BY_VALUE, BY_NAME }
    final boolean byCalled; // true=flame graph; false=root graph
//    final IODirection direction;
//    final IOUnit unit;
    final Order order;
    final WidthScale scale;
    final float minPercent;

    public DisplayOptions() {
        this(false, Order.BY_NAME, WidthScale.VALUE, 0f);
    }

    public DisplayOptions(boolean byCalled, Order order, WidthScale scale, float minPercent) {
        this.byCalled = byCalled;
        this.order = order;
        this.scale = scale;
        this.minPercent = minPercent;
    }
}