package com.dell.gumshoe.inspector;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.inspector.Tool.GUIComponents;
import com.dell.gumshoe.inspector.graph.StackGraphPanel;
import com.dell.gumshoe.inspector.tools.AboutPanel;
import com.dell.gumshoe.inspector.tools.FilterEditor;
import com.dell.gumshoe.inspector.tools.ProbeSourcePanel;
import com.dell.gumshoe.inspector.tools.SampleFileChooser;
import com.dell.gumshoe.inspector.tools.SampleSelectionListener;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.BorderLayout;
import java.util.Map;

/** inspector GUI components
 *  create and connect gumshoe components
 */
public class GUI extends JPanel implements GUIComponents, SampleSelectionListener {
    private final JFrame frame;
    private final SampleFileChooser fileSource;
    private final ProbeSourcePanel probeSource;
    private final FilterEditor filterEditor;
    private final StackGraphPanel graph;
    private final JPanel detailPanel;
    private final JPanel aboutPanel = new AboutPanel();

    private SampleSource currentSource;

    public GUI(final JFrame frame, ProbeManager probe, boolean hasMain) {
        this.frame = frame;
        this.fileSource = new SampleFileChooser(); // FileSourcePanel();
        fileSource.addListener(this);

        if(probe==null) {
            this.probeSource = null;
        } else {
            this.probeSource = new ProbeSourcePanel(probe);
            this.probeSource.addListener(this);
            currentSource = probeSource;
        }

        this.graph = new StackGraphPanel();
        /////

        JToolBar toolbar = new JToolBar();
        for(Tool tool : Tool.values()) {
            toolbar.add(tool.getButton());
        }
        Tool.setTargetComponents(this);
        Tool.LOAD_NEXT_SAMPLE.getButton().setEnabled(false);
        Tool.LOAD_PREVIOUS_SAMPLE.getButton().setEnabled(false);

        detailPanel = new JPanel();
        detailPanel.setLayout(new BorderLayout());
        final JComponent detailField = graph.getDetailField();
        final JScrollPane scroll = new JScrollPane(detailField);
        detailPanel.add(scroll, BorderLayout.CENTER);

        filterEditor = new FilterEditor();
        filterEditor.setGraph(graph);

//        final JPanel graphPanel = new JPanel();
//        graphPanel.setLayout(new BorderLayout());
//        graphPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
//        graphPanel.add(graph);

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(graph), BorderLayout.CENTER);
    }

    /////

    public JFrame getFrame() { return frame; }
    public JComponent getProbeControl() { return probeSource; }
    public JFileChooser getFileControl() { return fileSource; }
    public JComponent getFilterControl() { return filterEditor; }
    public JComponent getStatisticControl() { return graph.getStatisticChooser(); }
    public JComponent getGraphControl() { return graph.getOptionEditor(); }
    public JComponent getDetailPanel() { return detailPanel; }

    @Override
    public JComponent getAboutPanel() {
        return aboutPanel;
    }

    @Override
    public void zoomMax() {
        graph.zoomMax();
    }

    @Override
    public void zoomFit() {
        graph.zoomFit();
    }

    @Override
    public void zoomIn() {
        graph.zoomIn();
    }

    @Override
    public void zoomOut() {
        graph.zoomOut();
    }

    @Override
    public void previousSample() {
        if(currentSource!=null) {
            currentSource.previousSample();
        }
    }

    @Override
    public void nextSample() {
        if(currentSource!=null) {
            currentSource.nextSample();
        }
    }

    /////

    private void togglePrevNextButtons() {
        Tool.LOAD_NEXT_SAMPLE.getButton().setEnabled(currentSource.hasNext());
        Tool.LOAD_PREVIOUS_SAMPLE.getButton().setEnabled(currentSource.hasPrevious());
    }
    @Override
    public void sampleWasSelected(Object source, String time, String type, Map<Stack, StatisticAdder> data) {
        // track source for prev and next buttons
        final boolean isProbeSource = source==probeSource;
        this.currentSource = isProbeSource ? probeSource : fileSource;
        togglePrevNextButtons();

        // show current sample in title bar
        frame.setTitle("Inspector: "+ type + " data @ " + time + " from " +(isProbeSource?"probe":source));

        // show stat options for current sample type
        graph.getStatisticChooser().sampleWasSelected(source, time, type, data);

        // display graph
        graph.updateModel(data);
    }

    // when source reports samples available has changed, enable or disable next/prev buttons
    @Override
    public void contentsChanged(SampleSource source) {
        if(source==currentSource) {
            togglePrevNextButtons();
        }
    }
}
