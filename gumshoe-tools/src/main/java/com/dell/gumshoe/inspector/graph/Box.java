package com.dell.gumshoe.inspector.graph;

import com.dell.gumshoe.inspector.helper.DataTypeHelper;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/** cached model for individual display rectangles each showing details of a stack frame */
class Box {
    public static final long RESOLUTION = 10000;

    private final int row;
    private float position, width;
    private final StackFrameNode boxNode;
    private final StackFrameNode parentNode;
    private String label;

    public Box(int row, float position, float width, StackFrameNode boxNode, StackFrameNode parentNode) {
        this.row = row;
        this.position = position;
        this.width = width;
        this.boxNode = boxNode;
        this.parentNode = parentNode;
    }

    public StackTraceElement getFrame() { return boxNode.getFrame(); }
    public float getPosition() { return position; }
    public float getWidth() { return width; }
    public StackFrameNode getNode() { return boxNode; }
    public StackFrameNode getParentNode() { return parentNode; }

    public Rectangle getBounds(float rowHeight, int displayWidth, int rowsMinusOne, DisplayOptions o) {
        final int boxX = (int) (position * displayWidth);
        final int boxWidth = (int)(width * displayWidth);
        final int boxY = (int) (rowHeight * (o.byCalled?(rowsMinusOne-row):row));
        final int boxHeight = (int)rowHeight;
        return new Rectangle(boxX, boxY, boxWidth, boxHeight);
    }

    public void draw(Graphics g, float rowHeight, int displayWidth, int rowsMinusOne, DisplayOptions o, Box selected) {
        final int boxX = (int) (position * displayWidth);
        final int boxWidth = (int)(width * displayWidth);
        final int boxY = (int) (rowHeight * (o.byCalled?(rowsMinusOne-row):row));
        final int boxHeight = (int)rowHeight;
        final boolean isSelected = this==selected;
        g.setClip(boxX, boxY, boxWidth+1, boxHeight+1);

        final Color baseColor = getColor(o);
        final Color color = isSelected ? baseColor.darker() : baseColor;

        g.setColor(color);
        g.fillRect(boxX, boxY, boxWidth, boxHeight);
        g.setColor(Color.BLACK);
        g.drawRect(boxX, boxY, boxWidth, boxHeight);
        g.drawString(getLabelText(), boxX+1, boxY+15);
        g.setClip(null);
    }

    public boolean contains(float rowHeight, int displayWidth, int rows, long total, DisplayOptions o, int x, int y) {
        // same box coordinates as draw(), but short circuit if possible
        final int boxX = (int) (position * displayWidth);
        if(x<boxX) { return false; }

        final int boxWidth = (int)(width * displayWidth);
        if(x>=boxX+boxWidth) { return false; }

        final int boxY = (int) (rowHeight * (o.byCalled?(rows-row-1):row));
        if(y<boxY) { return false; } // can avoid remaining calculations

        final int boxHeight = (int)rowHeight;
        if(y>=boxY+boxHeight) { return false; }

        return true;
    }

    private Color getColor(DisplayOptions o) {
        //  (50%,100%]--> 0,  (33%,50%]-->1, (25%,33%]-->2,... (1/N,1/(N-1)]-->(N-2)
        int index = (int) (1f/width)-1;
        index = Math.min(Math.max(0, index), StackGraphPanel.BOX_COLORS.length-1);
        return StackGraphPanel.BOX_COLORS[index];
    }

    public String getLabelText() {
        if(label==null) {
            final String[] parts = boxNode.getFrame().getClassName().split("\\.");
            final StringBuilder labelText = new StringBuilder(parts[parts.length-1]);

            // the rest may be missing due to simplification filter
            final String methodName = boxNode.getFrame().getMethodName();
            if(methodName.length()>0) {
                labelText.append(".").append(methodName);
            }
            final int lineNumber = boxNode.getFrame().getLineNumber();
            if(lineNumber>0) {
                labelText.append(":").append(lineNumber);
            }
            label = labelText.toString();
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
        return String.format("%3d %.2f %.2f %s", row, position, width, boxNode.getFrame().toString());
    }
}