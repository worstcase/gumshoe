package com.dell.gumshoe.inspector;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.inspector.StatisticsSourcePanel.Listener;
import com.dell.gumshoe.inspector.graph.StackGraphPanel;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import static com.dell.gumshoe.util.Swing.*;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/** gumshoe main window and launcher
 *
 *  running as an application,
 *  this will start gumshoe GUI in a frame and then look for arguments.
 *  first argument is the name of the target main class to run,
 *  remaining arguments are passed as the args to that main.
 */
public class Inspector extends JPanel {
    public static void main(String[] args) throws Throwable {
        final boolean hasMain = args.length>0;
        launchGumshoe(hasMain);
        if(hasMain) {
            launchMain(args);
        }
    }

    private static void launchGumshoe(boolean hasMain) throws Exception {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // not available? will use default L&F
        }

        final boolean forceProbe = Boolean.getBoolean("gumshoe.probe.enabled");
        final boolean useProbe = forceProbe || hasMain;
        final ProbeManager probe = useProbe ? new ProbeManager() : null;
        if(useProbe) { probe.initialize(); }

        final Inspector gui = new Inspector(probe, hasMain);
        gui.setVisible(true);
    }

    private static void launchMain(String[] args) throws Throwable {
        final String mainClass = args[0];
        final String[] newArgs = new String[args.length-1];
        if(args.length>1) {
            System.arraycopy(args, 1, newArgs, 0, args.length-1);
        }
        launchMain(mainClass, newArgs);
    }

    private static void launchMain(String mainClassName, String[] args) throws Throwable {
        final Class mainClass = Class.forName(mainClassName);
        final Method mainMethod = mainClass.getDeclaredMethod("main", args.getClass());
        try {
            mainMethod.invoke(mainClass, new Object[] { args });
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /////

    private final JFrame frame = new JFrame();

    public Inspector() {
        this(null, false);
    }

    private Inspector(ProbeManager probe, boolean hasMain) {
        final StackGraphPanel graph = new StackGraphPanel();

        final StatisticsSourcePanel statsRelay = new StatisticsSourcePanel(probe);
        statsRelay.addListener(new Listener() {
            @Override
            public void statisticsLoaded(String time, String type, Map<Stack,StatisticAdder> stats) {
                if(type!=null) {
                    graph.getOptionEditor().chooseStatType(type);
                    frame.setTitle("Gumshoe: displaying " + type + " from " + time);
                    graph.updateModel(stats);
                } else {
                    frame.setTitle("Gumshoe");
                }
            }
        });

        final JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new BorderLayout());
        final JComponent detailField = graph.getDetailField();
        final JScrollPane scroll = new JScrollPane(detailField);
        detailPanel.add(scroll, BorderLayout.CENTER);

        final FilterEditor filterEditor = new FilterEditor();
        filterEditor.setGraph(graph);

        final JTabbedPane settings = createTabbedPaneWithoutHScroll();
        settings.setBorder(BorderFactory.createEmptyBorder(10,5,5,5));
        settings.addTab("Collect -->", statsRelay);
        settings.addTab("--> Filter -->", filterEditor);
        settings.addTab("--> Display -->", graph.getOptionEditor());
        settings.addTab("--> Examine", detailPanel);

        final  JPanel settingPanel = new JPanel();
        settingPanel.setLayout(new BorderLayout());
        settingPanel.add(settings);

        final JPanel graphPanel = new JPanel();
        graphPanel.setLayout(new BorderLayout());
        graphPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        graphPanel.add(graph);

        final JSplitPane mainView = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainView.setTopComponent(graphPanel);
        mainView.setBottomComponent(new JScrollPane(settings, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

        setLayout(new BorderLayout());
        add(mainView, BorderLayout.CENTER);

        if( ! hasMain) {
            // if this is the only program, click X to exit (otherwise just hide gumshoe)
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }

        frame.getContentPane().add(this);
        frame.pack();
        frame.setTitle("Gumshoe");
    }

    public void setVisible(boolean visible) {
        frame.setVisible(true);
    }
}
