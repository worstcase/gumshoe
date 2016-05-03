package com.dell.gumshoe.inspector;

import com.dell.gumshoe.inspector.tools.HasCloseButton;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

public enum Tool implements ActionListener {
    CONFIGURE_PROBE("Select samples from this VM", "probe.png", "Manage Probes") {
        public void setComponents(GUIComponents components) {
            super.setComponents(components);
            if(getPopup()==null) {
                getButton().setEnabled(false);
            }
        }
        protected JDialog getPopup() { return targets.getProbePopup(); }
    },

    OPEN_FILE("Read data file", "open.png", "Open Data File") {
        public void actionPerformed(ActionEvent e) {
            final JFileChooser chooser = targets.getFileChooser();
            if(chooser==null || chooser.isShowing()) { return; }
            chooser.showOpenDialog(targets.getFrame());
        }
    },

    LOAD_PREVIOUS_SAMPLE("Previous sample", "prev.png") {
        public void actionPerformed(ActionEvent e) {
            targets.prevSample();
        }
    },

    LOAD_NEXT_SAMPLE("Next sample", "next.png") {
        public void actionPerformed(ActionEvent e) {
            targets.nextSample();
        }
    },

    CONFIGURE_FILTERS("Stack filters", "filters.png", "Stack Filter Configuration") {
        protected JDialog getPopup() { return targets.getFilterPopup(); }
    },
    CHOOSE_STATISTIC("Choose statistic", "stats.png", "Statistic to Display") {
        protected JDialog getPopup() { return targets.getStatisticPopup(); }
    },
    CONFIGURE_GRAPH("Configure graph", "graph.png", "Graph Display Options") {
        protected JDialog getPopup() { return targets.getGraphPopup(); }
    },
    ZOOM_IN("Bigger", "larger.png") {
        public void actionPerformed(ActionEvent e) {
            targets.zoomIn();
        }
    },
    ZOOM_FIT("Zoom to fit", "resize.png") {
        public void actionPerformed(ActionEvent e) {
            targets.zoomFit();
        }
    },
    ZOOM_OUT("Smaller", "smaller.png") {
        public void actionPerformed(ActionEvent e) {
            targets.zoomOut();
        }
    },

    TOGGLE_DETAIL_PANEL("Examine selected frame", "detail.png", "Stack Frame Details") {
        protected JDialog getPopup() { return targets.getDetailPopup(); }
    },
    ABOUT_INSPECTOR("About gumshoe", "about.png", "About Gumshoe Inspector") {
        protected JDialog getPopup() { return targets.getAboutPopup(); }
    };

    /////

    protected final ToolActionHelper targets;
    protected final JButton button;

    private Tool(String label, String iconFileName) {
        this(label, iconFileName, null);
    }

    private Tool(String label, String iconFileName, String title) {
        final URL url = getClass().getResource("/" + iconFileName);
        if(url==null) {
            button = new JButton(label);
        } else {
            final ImageIcon icon = new ImageIcon(url, label);
            button = new JButton(icon);
            button.setToolTipText(label);
        }
        button.addActionListener(this);
        targets = new ToolActionHelper(title);
    }

    public void setComponents(GUIComponents components) {
        targets.setProxy(components);
    }

    public AbstractButton getButton() { return button; }

    /////

    protected Window getPopup() {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        final Window window = getPopup();
        if(window==null || window.isShowing()) { return; }
        window.pack();
        window.setVisible(true);
    }

    /////

    public static void setTargetComponents(GUIComponents components) {
        for(Tool tool : Tool.values()) {
            tool.setComponents(components);
        }
    }

    public static class ToolActionHelper {
        private final String windowTitle;
        private GUIComponents gui;
        private JDialog popup;

        public ToolActionHelper() { this(null); }
        public ToolActionHelper(String title) { this.windowTitle = title; }
        public void setProxy(GUIComponents gui) { this.gui = gui; }

        /////

        public JFrame getFrame() {
            return gui.getFrame();
        }

        public JDialog getProbePopup() {
            if(popup==null) {
                popup = createDialog(windowTitle, gui.getProbeControl());
            }
            return popup;
        }

        public JFileChooser getFileChooser() {
            return gui.getFileControl();
        }

        public JDialog getFilterPopup() {
            if(popup==null) {
                popup = createDialog(windowTitle, gui.getFilterControl());
            }
            return popup;
        }

        public JDialog getStatisticPopup() {
            if(popup==null) {
                popup = createDialog(windowTitle, gui.getStatisticControl());
            }
            return popup;
        }

        public JDialog getGraphPopup() {
            if(popup==null) {
                popup = createDialog(windowTitle, gui.getGraphControl());
            }
            return popup;
        }

        public JDialog getDetailPopup() {
            if(popup==null) {
                popup = createDialog(windowTitle, gui.getDetailPanel());
            }
            return popup;
        }

        public JDialog getAboutPopup() {
            if(popup==null) {
                popup = createDialog(windowTitle, gui.getAboutPanel());
            }
            return popup;
        }

        public JDialog createDialog(String title, JComponent contents) {
            if(gui==null || contents==null) { return null; }
            final JDialog probePopup = new JDialog(gui.getFrame());
            probePopup.setTitle(title);
            probePopup.getContentPane().add(contents);
            if(contents instanceof HasCloseButton) {
                final HasCloseButton notifier = (HasCloseButton)contents;
                notifier.addCloseListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        probePopup.setVisible(false);
                    }
                });
            }
            return probePopup;
        }

        /////

        public void zoomFit() { gui.zoomFit(); }
        public void zoomMax() { gui.zoomMax(); }
        public void zoomIn() { gui.zoomIn(); }
        public void zoomOut() { gui.zoomOut(); }
        public void prevSample() { gui.previousSample(); }
        public void nextSample() { gui.nextSample(); }
    }

    public static interface GUIComponents {
        public JFrame getFrame();
        public JComponent getProbeControl();
        public JFileChooser getFileControl();
        public JComponent getFilterControl();
        public JComponent getStatisticControl();
        public JComponent getGraphControl();
        public JComponent getDetailPanel();
        public JComponent getAboutPanel();
        public void zoomMax();
        public void zoomFit();
        public void zoomIn();
        public void zoomOut();
        public void previousSample();
        public void nextSample();
    }
}