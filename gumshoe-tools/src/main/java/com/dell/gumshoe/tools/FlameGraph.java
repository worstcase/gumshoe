package com.dell.gumshoe.tools;

import com.dell.gumshoe.socket.SocketIOListener.DetailAccumulator;
import com.dell.gumshoe.stack.Filter;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ToolTipManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** visualization of IO statistics per call stack
 *
 * similar to a flame graph, but width of each box represents the amount of IO,
 * depth shows call stack
 *
 */
public class FlameGraph extends JPanel {
    private Options options;
    private Map<Stack, DetailAccumulator> values;
    private TreeNode model;
    private transient List<Box> boxes;
    private OptionEditor optionEditor;
    private StackFilter filter;
    private JTextArea detailField;
    private StackTraceElement selectedFrame;

    private static void debug(String msg) {
        System.out.println(msg);
    }

    public FlameGraph() {
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                updateDetails(e);
            }
        });
    }

    private static class TreeNode {
        private long value;
        private StackTraceElement frame;
        private DetailAccumulator detail = new DetailAccumulator();
        private Map<StackTraceElement,TreeNode> divisions = new LinkedHashMap<>();

        private TreeNode() { }

        public TreeNode(Map<Stack, DetailAccumulator> values, Options options, StackFilter filter) {
            for(Map.Entry<Stack, DetailAccumulator> entry : values.entrySet()) {
                final Stack origStack = entry.getKey();
                final DetailAccumulator details = entry.getValue();
                final Stack displayStack = filter==null ? origStack : origStack.applyFilter(filter);
                final List<StackTraceElement> frames = Arrays.asList(displayStack.getFrames());
                if(options.byCalled) { Collections.reverse(frames); }

                TreeNode node = this;
                node.detail.add(details);
                final long nodeValue = getValue(details, options);
                node.value += nodeValue;
                for(StackTraceElement frame : frames) {
                    TreeNode frameNode = node.divisions.get(frame);
                    if(frameNode==null) {
                        frameNode = new TreeNode();
                        frameNode.value = nodeValue;
                        frameNode.frame = frame;
                        node.divisions.put(frame, frameNode);
                    } else {
                        frameNode.value += nodeValue;
                    }
                    node = frameNode;
                    node.detail.add(details);
                }
            }
        }

        private long getValue(DetailAccumulator details, Options options) {
            final WidthScale width = options.scale;
            switch(width) {
                case EQUAL:     return 5;
                case VALUE:     return getStatValue(details, options);
                case LOG_VALUE:
                default:        return (long)(100. * Math.log1p(getStatValue(details, options)));
            }
        }

        private long getStatValue(DetailAccumulator details, Options options) {
            final IOStat stat = options.direction;
            final IOUnit value = options.value;
            switch(stat) {
                case READ:              return getReadValue(details, value);
                case WRITE:             return getWriteValue(details, value);
                case READ_PLUS_WRITE:
                default:                return getReadValue(details, value) + getWriteValue(details, value);
            }
        }

        private long getReadValue(DetailAccumulator details, IOUnit unit) {
            switch(unit) {
                case OPS:   return details.readCount.get();
                case BYTES: return details.readBytes.get();
                case TIME:
                default:    return details.readTime.get();
            }
        }

        private long getWriteValue(DetailAccumulator details, IOUnit unit) {
            switch(unit) {
                case OPS:   return details.writeCount.get();
                case BYTES: return details.writeBytes.get();
                case TIME:
                default:    return details.writeTime.get();
            }
        }

        public int getDepth(Options options) {
            final long value = getValue(detail, options);
            if(value==0) { return 0; }
            int max = -1;
            for(TreeNode div : divisions.values()) {
                max = Math.max(div.getDepth(options), max);
            }
            return max + 1;
        }

        private List<StackTraceElement> getDivisions(final TreeNode model, Order order) {
            final List<StackTraceElement> keys = new ArrayList<>(model.divisions.keySet());
            if(order==Order.BY_VALUE) {
                Collections.sort(keys, new Comparator<StackTraceElement>() {
                    @Override
                    public int compare(StackTraceElement key1, StackTraceElement key2) {
                        return -Long.valueOf(model.divisions.get(key1).value).compareTo(Long.valueOf(model.divisions.get(key2).value));
                    }
                });
            }
            return keys;
        }

        public List<Box> createBoxes(Options options) {
            final List<Box> out = new ArrayList<Box>();
            final int depth = getDepth(options);
            Map<TreeNode,Long> priorRowModels = Collections.singletonMap(this, 0L);
            for(int row = 0;row<depth;row++) {
                final Map<TreeNode,Long> thisRowModels = new LinkedHashMap<>();
                for(Map.Entry<TreeNode,Long> parentEntry : priorRowModels.entrySet()) {
                    final long parentPosition = parentEntry.getValue();
                    final TreeNode parent = parentEntry.getKey();
                    final List<StackTraceElement> keys = getDivisions(parent, options.order);
                    debug("creating boxes: row " + row + " parent " + parent.value + " divs " + keys.size());

                    // first division left-aligned to parent position
                    long position = parentPosition;
                    for(StackTraceElement key : keys) {
                        final TreeNode divModel = parent.divisions.get(key);
                        debug("creating boxes: row " + row + " parent " + parent.value + " child " + key + " " + divModel.value);
                        if(divModel.value>0)  {
                            out.add(new Box(row, position, divModel, parent));
                            thisRowModels.put(divModel, position);
                            position += divModel.value;
                        }
                    }
                }
                priorRowModels = thisRowModels;
            }
            debug("create " + out.size() + " boxes, depth " + depth + " from\n" + this);
            return out;
        }

        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder();
            addNode(msg, "");
            return msg.toString();
        }

        private void addNode(StringBuilder builder, String indent) {
            for(Map.Entry<StackTraceElement,TreeNode> entry : divisions.entrySet()) {
                builder.append(indent)
                       .append(entry.getKey())
                       .append(" ")
                       .append(entry.getValue().detail)
                       .append("\n");
                entry.getValue().addNode(builder, indent + " ");
            }
        }
    }

    private static class Box {
        private final int row;
        private final long position;
        private final TreeNode boxNode, parentNode;

        public Box(int row, long position, TreeNode boxNode, TreeNode parentNode) {
            this.row = row;
            this.position = position;
            this.boxNode = boxNode;
            this.parentNode = parentNode;
        }

        public void draw(Graphics g, int displayHeight, int dispalyWidth, int rows, long total, Options o, StackTraceElement selected) {
            final float rowHeight = displayHeight / (float)rows;
            final float unitWidth = dispalyWidth / (float)total;
            final int boxX = (int) (position * unitWidth);
            final int boxWidth = (int) (boxNode.value * unitWidth);
            final int boxY = (int) (rowHeight * (o.byCalled?(rows-row-1):row));
            final int boxHeight = (int)rowHeight;
            final boolean isSelected = this.boxNode.frame==selected;

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

        public boolean contains(int displayHeight, int dispalyWidth, int rows, long total, Options o, int x, int y) {
            final float rowHeight = displayHeight / (float)rows;
            final float unitWidth = dispalyWidth / (float)total;
            final int boxX = (int) (position * unitWidth);
            final int boxWidth = (int) (boxNode.value * unitWidth);
            final int boxY = (int) (rowHeight * (o.byCalled?(rows-row-1):row));
            final int boxHeight = (int)rowHeight;

            return x>=boxX && x<boxX+boxWidth && y>=boxY && y<boxY+boxHeight;
        }

        private Color getColor(long total, Options o) {
            int index = (int) (total/boxNode.value)-2; //  >50%--> 0,  >33%-->1, >25%-->2, > (1/N)-->(N-2)
            if(index<0) index=0;
            if(index>=BOX_COLORS.length) index=BOX_COLORS.length-1;
            return BOX_COLORS[index];
        }

        public String getLabelText() {
            final String[] parts = boxNode.frame.getClassName().split("\\.");
            final String className = parts[parts.length-1].replaceAll("\\$", ".");
            return String.format("%s.%s:%d", className, boxNode.frame.getMethodName(), boxNode.frame.getLineNumber());
        }

        public String getToolTipText() {
            return String.format("<html>\n"
                                    + "%s<br>\n"
                                    + "read %d ops%s<br>\n"
                                    + "read %d bytes%s<br>\n"
                                    + "read time %d ms%s<br>\n"
                                    + "write %d ops%s<br>\n"
                                    + "write %d bytes%s<br>\n"
                                    + "write time %d ms%s\n"
                                    + "<html>",
                                  boxNode.frame,
                                  boxNode.detail.readCount.get(), getPercent(boxNode.detail.readCount, parentNode.detail.readCount),
                                  boxNode.detail.readBytes.get(), getPercent(boxNode.detail.readBytes, parentNode.detail.readBytes),
                                  boxNode.detail.readTime.get(), getPercent(boxNode.detail.readTime, parentNode.detail.readTime),
                                  boxNode.detail.writeCount.get(), getPercent(boxNode.detail.writeCount, parentNode.detail.writeCount),
                                  boxNode.detail.writeBytes.get(), getPercent(boxNode.detail.writeBytes, parentNode.detail.writeBytes),
                                  boxNode.detail.writeTime.get(), getPercent(boxNode.detail.writeTime, parentNode.detail.writeTime));
        }

        public String getDetailText(Options o) {

            return String.format("Frame: %s\n"
                                    + "Read: %d operations%s, %d bytes%s, %d ms %s\n"
                                    + "Write: %d operations%s, %d bytes%s, %d ms %s\n\n"
                                    + (o.byCalled ? "Calls %d methods: %s" : "Called by %d methods: %s"),
                                    boxNode.frame,
                                    boxNode.detail.readCount.get(), getPercent(boxNode.detail.readCount, parentNode.detail.readCount),
                                    boxNode.detail.readBytes.get(), getPercent(boxNode.detail.readBytes, parentNode.detail.readBytes),
                                    boxNode.detail.readTime.get(), getPercent(boxNode.detail.readTime, parentNode.detail.readTime),
                                    boxNode.detail.writeCount.get(), getPercent(boxNode.detail.writeCount, parentNode.detail.writeCount),
                                    boxNode.detail.writeBytes.get(), getPercent(boxNode.detail.writeBytes, parentNode.detail.writeBytes),
                                    boxNode.detail.writeTime.get(), getPercent(boxNode.detail.writeTime, parentNode.detail.writeTime),
                                    boxNode.divisions.size(), getFrames(boxNode.divisions.keySet()) );

        }

        private String getFrames(Set<StackTraceElement> frames) {
            final StringBuilder out = new StringBuilder();
            for(StackTraceElement frame : frames) {
                out.append("\n ").append(frame);
            }
            return out.toString();
        }


        private String getPercent(Number num, Number div) {
            if(div.longValue()==0) { return ""; }
            return " " + (100*num.longValue()/div.longValue()) + "%";
        }
    }

    private static final Color[] BOX_COLORS = { Color.RED, Color.ORANGE, Color.YELLOW, Color.GRAY, Color.WHITE };

    public enum IOStat { READ, WRITE, READ_PLUS_WRITE }
    public enum IOUnit { OPS, BYTES, TIME }
    public enum Order { BY_VALUE, BY_NAME }
    public enum WidthScale { VALUE, LOG_VALUE, EQUAL }

    public static class Options {
        private final boolean byCalled;
        private final StackFilter stackFilter;
        private final IOStat direction;
        private final IOUnit value;
        private final Order order;
        private final WidthScale scale;
        public Options() {
            this(false, Filter.NONE, IOStat.READ, IOUnit.BYTES, Order.BY_NAME, WidthScale.VALUE);
        }

        public Options(boolean byCalled, StackFilter stackFilter, IOStat direction, IOUnit value, Order order, WidthScale scale) {
            this.byCalled = byCalled;
            this.stackFilter = stackFilter;
            this.direction = direction;
            this.value = value;
            this.order = order;
            this.scale = scale;
        }
    }

    public void updateOptions(Options o) {
        boxes = null;
        model = null;
        this.options = o;
        debug("setting options");
        repaint();
    }

    public void updateModel(Map<Stack, DetailAccumulator> values) {
        boxes = null;
        model = null;
        this.values = values;
        debug("setting model " + values.size());
        repaint();
    }

    public void setFilter(StackFilter filter) {
        this.filter = filter;
        boxes = null;
        model = null;
        repaint();
    }

    private void update() {
        model = new TreeNode(values, options, filter);
        debug("updated model:\n" + model);
        boxes = model.createBoxes(options);
        updateDetails();
    }

    @Override
    public void paintComponent(Graphics g) {
        debug("painting");
        try {
            final Dimension dim = getSize();
            g.setColor(getBackground());
            g.fillRect(0, 0, dim.width, dim.height);

            if(options==null || values==null) { return; }
            if(boxes==null) {
                update();
            }

            int rows = model.getDepth(options);
            long total = model.value;
            for(Box box : boxes) {
                box.draw(g, dim.height-RULER_HEIGHT, dim.width, rows, total, options, selectedFrame);
            }
            paintRuler(g, dim.height, dim.width);
        } catch(Exception e) {
            e.printStackTrace();
        }

    }


    private static final int RULER_HEIGHT = 25;
    private static final int RULER_MAJOR_HEIGHT = 15;
    private static final int RULER_MINOR_HEIGHT = 5;
    private static final int RULER_MAJOR = 4;
    private static final int RULER_MINOR = 20;
    private void paintRuler(Graphics g, int height, int width) {
        for(int i=1; i<RULER_MINOR; i++) {
            int x = (width-1)*i/RULER_MINOR;
            g.drawLine(x, height-1, x, height-RULER_MINOR_HEIGHT);
        }
        for(int i=1; i<RULER_MAJOR; i++) {
            int x = (width-1)*i/RULER_MAJOR;
            g.drawLine(x, height-1, x, height-RULER_MAJOR_HEIGHT);
        }
        g.drawLine(0, height-1, width-1, height-1);
    }

    public JComponent getOptionEditor() {
        if(optionEditor==null) {
            optionEditor = new OptionEditor();
            optionEditor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateOptions(optionEditor.getOptions());
                }
            });
        }
        return optionEditor;
    }

    public JComponent getDetailField() {
        if(detailField==null) {
            detailField = new JTextArea();
        }
        return detailField;
    }

    private Box getBoxFor(MouseEvent e) {
        if(boxes==null) {
            return null;
        }

        final Dimension dim = getSize();
        int rows = model.getDepth(options);
        long total = model.value;
        for(Box box : boxes) {
            if(box.contains(dim.height-RULER_HEIGHT, dim.width, rows, total, options, e.getX(), e.getY())) {
                return box;
            }

        }
        return null;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        final Box box = getBoxFor(event);
        return box==null ? "" : box.getToolTipText();
    }

    private void updateDetails() {
        for(Box box : boxes) {
            if(box.boxNode.frame==selectedFrame) {
                updateDetails(box);
                return;
            }
        }
    }

    private void updateDetails(MouseEvent event) {
        final Box box = getBoxFor(event);
        if(box!=null) {
            updateDetails(box);
        }
    }

    private void updateDetails(Box box) {
        selectedFrame = box.boxNode.frame;
        detailField.setText(box.getDetailText(options));
        repaint();
    }
}
