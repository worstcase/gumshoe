package com.dell.gumshoe.inspector.graph;

import com.dell.gumshoe.inspector.helper.DataTypeHelper;
import com.dell.gumshoe.inspector.tools.OptionEditor;
import com.dell.gumshoe.inspector.tools.StatisticChooser;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.StatisticAdder;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** visualization of IO statistics per call stack
 *
 * similar to a flame graph, but width of each box represents the amount of IO,
 * depth shows call stack
 *
 */
public class StackGraphPanel extends JPanel implements Scrollable {
    // color boxes for: 50%, 33%, 25%, less
//    static final Color[] BOX_COLORS = { Color.RED, Color.ORANGE, Color.YELLOW, Color.LIGHT_GRAY, Color.WHITE };
    static final Color[] BOX_COLORS = { Color.RED, Color.ORANGE, Color.YELLOW, Color.WHITE };
    private static final double ZOOM_BASE = 2;

//    public static final int RULER_HEIGHT = 25;
//    private static final int RULER_MAJOR_HEIGHT = 15;
//    private static final int RULER_MINOR_HEIGHT = 5;
//    private static final int RULER_MAJOR = 4;
//    private static final int RULER_MINOR = 20;


    ///// data model and public API

    private Map<String,DataTypeHelper> helpers = new HashMap<>();

    private DisplayOptions options;
    private Map<Stack, StatisticAdder> values;
    private int modelRows;
    private long modelValueTotal;
    private transient StackFrameNode model;
    private transient List<Box> boxes;
    private OptionEditor optionEditor = new OptionEditor();
    private StackFilter filter;
    private JTextArea detailField;
    private Box selectedBox;

    private BufferedImage image;
    private float zoom = 0;
    private Dimension baseSize, zoomedSize;
    private float lastBoxHeight;
    private float lastTextHeight;

    private List<ListSelectionListener> listeners = new CopyOnWriteArrayList<>();
    public StackGraphPanel() {
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                updateDetails(e);
            }
        });

        updateOptions(optionEditor.getOptions());
        optionEditor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateOptions(optionEditor.getOptions());
            }
        });
    }

    public void addSelectionListener(ListSelectionListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        final ListSelectionEvent event = new ListSelectionEvent(this, 0, 0, false);
        for(ListSelectionListener listener : listeners) {
            listener.valueChanged(event);
        }
    }

    public StackFrameNode getSelection() { return selectedBox.getNode(); }
    public StackFrameNode getSelectionParent() { return selectedBox.getParentNode(); }

    public void updateOptions(DisplayOptions o) {
        if(this.options!=o) {
            this.options = o;
            this.model = null;
            this.boxes = null;
            this.image = null;
            repaint();
        }
    }

    public void updateModel(Map<Stack,StatisticAdder> values) {
        if(this.values!=values) {
            this.values = values;
            this.model = null;
            this.boxes = null;
            this.image = null;
            repaint();
        }
    }

    public void setFilter(StackFilter filter) {
        if(this.filter!=filter) {
            this.filter = filter;
            this.model = null;
            this.boxes = null;
            this.image = null;
            repaint();
        }
    }

    private StatisticChooser statChooser;

    public StatisticChooser getStatisticChooser() {
        if(statChooser==null) {
            statChooser = new StatisticChooser(this);
        }
        return statChooser;
    }

    public OptionEditor getOptionEditor() {
        return optionEditor;
    }

    public JComponent getDetailField() {
        if(detailField==null) {
            detailField = new JTextArea();
            detailField.setRows(40);
            detailField.setColumns(60);
        }
        return detailField;
    }

    ///// AWT event handling

    private void debugModel() {
//        System.out.println(model);
    }

    private void debugBoxes() {
//        System.out.println("boxes:");
//        for(Box box : boxes) {
//            System.out.println(box);
//        }
    }

    public BufferedImage getImage() {
        return image;
    }

    private synchronized void update() {
        if(model==null) {
            model = new StackFrameNode(values, options, filter);
            modelRows = model.getDepth(options);
            boxes = null;
            debugModel();
        }
        if(boxes==null) {
            modelValueTotal = model.getValue();
            boxes = model.createBoxes(options);
            updateDetails();
            debugBoxes();
        }
        if(image==null) {
            image = createImage();
        }
    }

    private class ImageUpdater extends SwingWorker<Object,Object> {
        @Override
        public Object doInBackground() throws Exception {
            update();
            repaint();
            return null;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        final Dimension dim = getSize();
        if(baseSize==null) {
            baseSize = new Dimension(dim);
            lastTextHeight = g.getFontMetrics(g.getFont()).getHeight();
        }
        if(options==null || values==null) {
            g.setColor(getBackground());
            g.fillRect(0,0,dim.width,dim.height);
            g.setColor(Color.WHITE);
            g.fillRect(10, 10, 350, 50);
            g.setColor(Color.BLACK);
            g.drawString("No data!  Load a report from a probe or file.", 30, 40);
        } else if(image==null || image.getHeight()!=dim.height || image.getWidth()!=dim.width) {
            g.setColor(getBackground());
            g.fillRect(0,0,dim.width,dim.height);
            g.setColor(Color.WHITE);
            g.fillRect(10, 10, 100, 50);
            g.setColor(Color.BLACK);
            g.drawString("Rendering...", 30, 40);
            new ImageUpdater().execute();
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
            final int displayHeight = dim.height; //-RULER_HEIGHT;
            final float rowHeight = displayHeight / (float)modelRows;
            final int rowsMinusOne = modelRows-1;
            final Shape clipBefore = g.getClip();
            for(Box box : boxes) {
                box.draw(g, rowHeight, dim.width, rowsMinusOne, options, selectedBox);
            }
            lastBoxHeight = ((float)modelRows)/displayHeight;
//            paintRuler(g, dim.height, dim.width);
            g.setClip(clipBefore);
        } finally {
            g.dispose();
        }
        return img;

    }

    private class ImageSelectionUpdater extends SwingWorker<Object,Object> {
        private final Box oldSelected, newSelected;

        public ImageSelectionUpdater(Box oldSelected, Box newSelected) {
            this.oldSelected = oldSelected;
            this.newSelected = newSelected;
        }
        @Override
        public Object doInBackground() throws Exception {
            updateSelectedPortion(oldSelected, newSelected);
            notifyListeners();
//            detailField.setText(newSelected.getDetailText());
//            //
            return null;
        }
    }

    private void updateImageSelection(Box oldSelected, Box newSelected) {
        if(boxes==null || image==null) return;
        new ImageSelectionUpdater(oldSelected, newSelected).execute();
    }

    private void updateSelectedPortion(Box oldSelected, Box newSelected) {
        if(boxes==null || image==null) return;
        final Dimension dim = getSize();
        final Graphics2D g = image.createGraphics();
        try {
            final int displayHeight = dim.height; //-RULER_HEIGHT;
            final float rowHeight = displayHeight / (float)modelRows;
            if(oldSelected!=null) {
                oldSelected.draw(g, rowHeight, dim.width, modelRows-1, options, newSelected);
                repaint(oldSelected.getBounds(rowHeight, dim.width, modelRows-1, options));
            }
            newSelected.draw(g, rowHeight, dim.width, modelRows-1, options, newSelected);
            repaint(newSelected.getBounds(rowHeight, dim.width, modelRows-1, options));
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            g.dispose();
        }
    }

//    private void paintRuler(Graphics g, int height, int width) {
//        if(options.scale==DisplayOptions.WidthScale.VALUE) {
//            for(int i=1; i<RULER_MINOR; i++) {
//                int x = (width-1)*i/RULER_MINOR;
//                g.drawLine(x, height-1, x, height-RULER_MINOR_HEIGHT);
//            }
//            for(int i=1; i<RULER_MAJOR; i++) {
//                int x = (width-1)*i/RULER_MAJOR;
//                g.drawLine(x, height-1, x, height-RULER_MAJOR_HEIGHT);
//            }
//            g.drawLine(0, height-1, width-1, height-1);
//        } else {
//            // show no ruler for other scales
//            g.setColor(getBackground());
//            g.fillRect(0, height-1, width, height-RULER_MAJOR_HEIGHT);
//        }
//    }

    private Box getBoxFor(MouseEvent e) {
        if(boxes==null) {
            return null;
        }

        final Dimension dim = getSize();
        final float rowHeight = (dim.height) / (float)modelRows;
        for(Box box : boxes) {
            if(box.contains(rowHeight, dim.width, modelRows, modelValueTotal, options, e.getX(), e.getY())) {
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
            if(box==selectedBox) {
                updateDetails(box);
                return;
            }
        }
    }

    private void updateDetails(MouseEvent event) {
        final Box box = getBoxFor(event);
        if(box!=null) {
            updateDetails(box);
//            repaint();
        }
    }

    private void updateDetails(Box box) {
        final Box oldSelection = selectedBox;
        selectedBox = box;
        updateImageSelection(oldSelection, selectedBox);
    }

    public Dimension getPreferredSize() {
        if(baseSize==null) { return super.getPreferredSize(); }
        if(zoomedSize==null) {
            final double multiplier = Math.pow(ZOOM_BASE, zoom);
            final double width = (baseSize.getWidth()) * multiplier;
            final double height = (baseSize.getHeight()) * multiplier;
            zoomedSize = new Dimension((int)width, (int)height);
        }
        return zoomedSize;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 100;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return baseSize==null;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return baseSize==null;
    }

    public void zoomMax() {
        final float ratio = lastTextHeight/lastBoxHeight; // need boxes this much larger
        final double offset = Math.log(ratio)/Math.log(ZOOM_BASE);
        zoom += offset;
        image = null;
        zoomedSize = null;
        revalidate();
    }

    public void zoomFit() {
        zoom = 0;
        image = null;
        zoomedSize = null;
        baseSize = null;
        revalidate();
    }


    public void zoomIn() {
        zoom++;
        image = null;
        zoomedSize = null;
        revalidate();
    }

    public void zoomOut() {
        zoom--;
        image = null;
        zoomedSize = null;
        revalidate();
    }
}
