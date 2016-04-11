package com.dell.gumshoe.tools.graph;

import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.StatisticAdder;
import com.dell.gumshoe.tools.OptionEditor;
import com.dell.gumshoe.tools.stats.DataTypeHelper;
import com.dell.gumshoe.tools.stats.SocketIOHelper;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ToolTipManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** visualization of IO statistics per call stack
 *
 * similar to a flame graph, but width of each box represents the amount of IO,
 * depth shows call stack
 *
 */
public class StackGraphPanel extends JPanel {
    // color boxes for: 50%, 25%, 12%, 6%, less
    static final Color[] BOX_COLORS = { Color.RED, Color.ORANGE, Color.YELLOW, Color.LIGHT_GRAY, Color.WHITE };

    private static final int RULER_HEIGHT = 25;
    private static final int RULER_MAJOR_HEIGHT = 15;
    private static final int RULER_MINOR_HEIGHT = 5;
    private static final int RULER_MAJOR = 4;
    private static final int RULER_MINOR = 20;


    ///// data model and public API

    private Map<String,DataTypeHelper> helpers = new HashMap<>();

    private DisplayOptions options;
    private Map<Stack, StatisticAdder> values;
    private int modelRows;
    private long modelValueTotal;
    private transient StackFrameNode model;
    private transient List<Box> boxes;
    private OptionEditor optionEditor;
    private StackFilter filter;
    private JTextArea detailField;
    private StackTraceElement selectedFrame;
    private BufferedImage image;

    public StackGraphPanel() {
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                updateDetails(e);
            }
        });
    }

    public void updateOptions(DisplayOptions o) {
        this.options = o;
        this.model = null;
        this.boxes = null;
        this.image = null;
        repaint();
    }

    public void updateModel(Map<Stack,StatisticAdder> values) {
        this.values = values;
        this.model = null;
        this.boxes = null;
        this.image = null;
        repaint();
    }

    public void setFilter(StackFilter filter) {
        this.filter = filter;
        this.model = null;
        this.boxes = null;
        this.image = null;
        repaint();
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

    ///// AWT event handling

    private class Updater implements Runnable {
        @Override
        public void run() {
            update();
            repaint();
        }
    }

    private void updateAsync() {
        final Thread t = new Thread(new Updater());
        t.setDaemon(true);
        t.run();
    }

    private void update() {
        if(model==null) {
            model = new StackFrameNode(values, options, filter);
            modelRows = model.getDepth(options);
            modelValueTotal = model.getValue();
            boxes = null;
        }
        if(boxes==null) {
            boxes = model.createBoxes(options);
            updateDetails();
        }
        image = createImage();
    }

    @Override
    public void paintComponent(Graphics g) {
        final Dimension dim = getSize();
        if(options==null || values==null) {
            g.drawString("No data", 10, 10);
        } else if(image==null || image.getHeight()!=dim.height || image.getWidth()!=dim.width) {
            g.drawString("Rendering...", 10, 10);
            updateAsync();
        } else {
            g.drawImage(image, 0, 0, null);
        }
    }

    private BufferedImage createImage() {
        // do we need to create new buffered image?
        final Dimension dim = getSize();
        final BufferedImage img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        try {
            g.setColor(getBackground());
            g.fillRect(0, 0, dim.width, dim.height);

            if(options==null || values==null) { return img; }

            if(boxes.isEmpty()) {
                g.drawString("No stack frames remain after filter", 10, 10);
            }
            for(Box box : boxes) {
                box.draw(g, dim.height-RULER_HEIGHT, dim.width, modelRows, modelValueTotal, options, selectedFrame);
            }
            paintRuler(g, dim.height, dim.width);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            g.dispose();
        }
        return img;

    }

    private void updateImageSelection(StackTraceElement oldSelected, StackTraceElement newSelected) {
        if(boxes==null || image==null) return;
        final Dimension dim = getSize();
        final Graphics2D g = image.createGraphics();
        for(Box box : boxes) {
            final StackTraceElement frame = box.boxNode.getFrame();
            if(frame.equals(oldSelected) || frame.equals(newSelected)) {
                box.draw(g, dim.height-RULER_HEIGHT, dim.width, modelRows, modelValueTotal, options, newSelected);
            }
        }
    }

    private void paintRuler(Graphics g, int height, int width) {
        if(options.scale==DisplayOptions.WidthScale.VALUE) {
            for(int i=1; i<RULER_MINOR; i++) {
                int x = (width-1)*i/RULER_MINOR;
                g.drawLine(x, height-1, x, height-RULER_MINOR_HEIGHT);
            }
            for(int i=1; i<RULER_MAJOR; i++) {
                int x = (width-1)*i/RULER_MAJOR;
                g.drawLine(x, height-1, x, height-RULER_MAJOR_HEIGHT);
            }
            g.drawLine(0, height-1, width-1, height-1);
        } else {
            // show no ruler for other scales
            g.setColor(getBackground());
            g.fillRect(0, height-1, width, height-RULER_MAJOR_HEIGHT);
        }
    }

    private Box getBoxFor(MouseEvent e) {
        if(boxes==null) {
            return null;
        }

        final Dimension dim = getSize();
        for(Box box : boxes) {
            if(box.contains(dim.height-RULER_HEIGHT, dim.width, modelRows, modelValueTotal, options, e.getX(), e.getY())) {
                return box;
            }

        }
        return null;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        final Box box = getBoxFor(event);
        return box==null ? null : box.getToolTipText();
    }

    private void updateDetails() {
        for(Box box : boxes) {
            if(box.boxNode.getFrame()==selectedFrame) {
                updateDetails(box);
                return;
            }
        }
    }

    private void updateDetails(MouseEvent event) {
        final Box box = getBoxFor(event);
        if(box!=null) {
            updateDetails(box);
            repaint();
        }
    }

    private void updateDetails(Box box) {
        final StackTraceElement oldSelection = selectedFrame;
        selectedFrame = box.boxNode.getFrame();
        updateImageSelection(oldSelection, selectedFrame);
        detailField.setText(box.getDetailText());
    }


}
