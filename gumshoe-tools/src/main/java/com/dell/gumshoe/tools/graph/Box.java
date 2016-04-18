package com.dell.gumshoe.tools.graph;

import com.dell.gumshoe.tools.stats.DataTypeHelper;

import java.awt.Color;
import java.awt.Graphics;

/** cached model for individual display rectangles each showing details of a stack frame */
class Box {
    private final int row;
    private final long position;
    final StackFrameNode boxNode;
    private final StackFrameNode parentNode;
    private String label;

    public Box(int row, long position, StackFrameNode boxNode, StackFrameNode parentNode) {
        this.row = row;
        this.position = position;
        this.boxNode = boxNode;
        this.parentNode = parentNode;
    }

    public void draw(Graphics g, int displayHeight, int displayWidth, int rows, long total, DisplayOptions o, StackTraceElement selected) {
        final float rowHeight = displayHeight / (float)rows;
        final float unitWidth = displayWidth / (float)total;
        final int boxX = (int) (position * unitWidth);
        final int boxWidth = (int) (boxNode.getValue() * unitWidth);
        final int boxY = (int) (rowHeight * (o.byCalled?(rows-row-1):row));
        final int boxHeight = (int)rowHeight;
        final boolean isSelected = this.boxNode.getFrame()==selected;

        g.setClip(boxX, boxY, boxWidth+1, boxHeight+1);

        final Color baseColor = getColor(total, o);
        final Color color = isSelected ? baseColor.darker() : baseColor;

        g.setColor(color);
        g.fillRect(boxX, boxY, boxWidth, boxHeight);
        g.setColor(Color.BLACK);
        g.drawRect(boxX, boxY, boxWidth, boxHeight);
        g.drawString(getLabelText(), boxX+1, boxY+15);
        g.setClip(null);
    }

    public boolean contains(int displayHeight, int displayWidth, int rows, long total, DisplayOptions o, int x, int y) {
        final float rowHeight = displayHeight / (float)rows;
        final int boxY = (int) (rowHeight * (o.byCalled?(rows-row-1):row));
        if(y<boxY) { return false; } // can avoid remaining calculations

        final int boxHeight = (int)rowHeight;
        if(y>=boxY+boxHeight) { return false; }

        final float unitWidth = displayWidth / (float)total;
        final int boxX = (int) (position * unitWidth);
        if(x<boxX) { return false; }

        final int boxWidth = (int) (boxNode.getValue() * unitWidth);
        return x<boxX+boxWidth;
    }

    private Color getColor(long total, DisplayOptions o) {
        int index = (int) (total/(boxNode.getValue()+1))-1; //  >50%--> 0,  >33%-->1, >25%-->2, > (1/N)-->(N-2)
        if(index<0) index=0;
        if(index>=StackGraphPanel.BOX_COLORS.length) index=StackGraphPanel.BOX_COLORS.length-1;
        return StackGraphPanel.BOX_COLORS[index];
    }

    public String getLabelText() {
        if(label==null) {
            final String[] parts = boxNode.getFrame().getClassName().split("\\.");
            final String className = parts[parts.length-1];
            final int lineNumber = boxNode.getFrame().getLineNumber();
            if(lineNumber>0) {
                label = String.format("%s.%s:%d", className, boxNode.getFrame().getMethodName(), lineNumber);
            } else {
                label = String.format("%s.%s", className, boxNode.getFrame().getMethodName());
            }
        }
        return label;
    }

    public String getToolTipText() {
        return getHelper().getToolTipText(boxNode, parentNode);
    }

    public String getDetailText() {
        return getHelper().getDetailText(boxNode, parentNode);
    }

    private DataTypeHelper getHelper() {
        return DataTypeHelper.forType(boxNode.getDetail().getType());
    }

    @Override
    public String toString() {
        return row + " " + position + " " + boxNode.getFrame();
    }
}