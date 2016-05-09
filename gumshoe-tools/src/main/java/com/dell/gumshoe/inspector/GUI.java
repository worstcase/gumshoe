package com.dell.gumshoe.inspector;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.inspector.Tool.GUIComponents;
import com.dell.gumshoe.inspector.graph.Ruler;
import com.dell.gumshoe.inspector.graph.StackGraphPanel;
import com.dell.gumshoe.inspector.tools.AboutPanel;
import com.dell.gumshoe.inspector.tools.DetailPanel;
import com.dell.gumshoe.inspector.tools.FilterEditor;
import com.dell.gumshoe.inspector.tools.ProbeSourcePanel;
import com.dell.gumshoe.inspector.tools.ReportFileChooser;
import com.dell.gumshoe.inspector.tools.ReportSelectionListener;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import java.awt.BorderLayout;
import java.util.Map;

/** inspector GUI components
 *  create and connect gumshoe components
 */
public class GUI extends JPanel implements GUIComponents, ReportSelectionListener {
    private final JFrame frame;
    private final ReportFileChooser fileSource;
    private final ProbeSourcePanel probeSource;
    private final FilterEditor filterEditor;
    private final StackGraphPanel graph;
    private final DetailPanel detailPanel;
    private final JPanel aboutPanel = new AboutPanel();

    private ReportSource currentSource;

    public GUI(final JFrame frame, ProbeManager probe, boolean hasMain) {
        this.frame = frame;
        this.fileSource = new ReportFileChooser(); // FileSourcePanel();
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
        Tool.LOAD_NEXT_REPORT.getButton().setEnabled(false);
        Tool.LOAD_PREVIOUS_REPORT.getButton().setEnabled(false);

        detailPanel = new DetailPanel();
        graph.addSelectionListener(detailPanel);

        filterEditor = new FilterEditor();
        filterEditor.setGraph(graph);

        final JScrollPane graphScroll = new JScrollPane(graph);
        graphScroll.setColumnHeaderView(new Ruler(graphScroll));

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(graphScroll, BorderLayout.CENTER);
    }

    /////

    public JFrame getFrame() { return frame; }
    public JComponent getProbeControl() { return probeSource; }
    public ReportFileChooser getFileControl() { return fileSource; }
    public JComponent getFilterControl() { return filterEditor; }
    public JComponent getStatisticControl() { return graph.getStatisticChooser(); }
    public JComponent getGraphControl() { return graph.getOptionEditor(); }
    public DetailPanel getDetailPanel() { return detailPanel; }

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
    public void previousReport() {
        if(currentSource!=null) {
            currentSource.previousReport();
        }
    }

    @Override
    public void nextReport() {
        if(currentSource!=null) {
            currentSource.nextReport();
        }
    }

    /////

    private void togglePrevNextButtons() {
        Tool.LOAD_NEXT_REPORT.getButton().setEnabled(currentSource.hasNext());
        Tool.LOAD_PREVIOUS_REPORT.getButton().setEnabled(currentSource.hasPrevious());
    }
    @Override
    public void reportWasSelected(Object source, String time, String type, Map<Stack, StatisticAdder> data) {
        // track source for prev and next buttons
        final boolean isProbeSource = source==probeSource;
        this.currentSource = isProbeSource ? probeSource : fileSource;
        togglePrevNextButtons();

        // show current report in title bar
        frame.setTitle("Reporting "+ type + " data @ " + time + " from " +(isProbeSource?"probe":source));

        // show stat options for current report type
        graph.getStatisticChooser().reportWasSelected(source, time, type, data);

        // display graph
        graph.updateModel(data);
    }

    // when reports available has changed, enable or disable next/prev buttons
    @Override
    public void contentsChanged(ReportSource source) {
        if(source==currentSource) {
            togglePrevNextButtons();
        }
    }
}
