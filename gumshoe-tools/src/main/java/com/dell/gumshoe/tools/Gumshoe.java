package com.dell.gumshoe.tools;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.tools.graph.StackGraphPanel;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static com.dell.gumshoe.tools.Swing.*;

/** gumshoe main window and launcher
 *
 *  running as an application,
 *  this will start gumshoe GUI in a frame and then look for arguments.
 *  first argument is the name of the target main class to run,
 *  remaining arguments are passed as the args to that main.
 */
public class Gumshoe extends JPanel {
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

        final JFrame frame = new JFrame();
        if( ! hasMain) {
            // if this is the only program, click X to exit (otherwise just hide gumshoe)
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }

        frame.getContentPane().add(new Gumshoe(probe));
        frame.pack();
        frame.setVisible(true);
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

    private Gumshoe(ProbeManager probe) {
        final StackGraphPanel graph = new StackGraphPanel();
        final StatisticsSourcePanel statsRelay = new StatisticsSourcePanel(graph, probe);

        final JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new BorderLayout());
        final JComponent detailField = graph.getDetailField();
        final JScrollPane scroll = new JScrollPane(detailField);
        detailPanel.add(scroll, BorderLayout.CENTER);

        final JPanel statsPanel = flow(statsRelay);

        final FilterEditor filterEditor = new FilterEditor();
        filterEditor.setGraph(graph);

        final JTabbedPane settings = new JTabbedPane();
        settings.setBorder(BorderFactory.createEmptyBorder(10,5,5,5));
        settings.addTab("Collect -->", statsPanel);
        settings.addTab("--> Filter -->", filterEditor);
        settings.addTab("--> Display -->", graph.getOptionEditor());
        settings.addTab("--> Examine", detailPanel);

        final JPanel graphPanel = new JPanel();
        graphPanel.setLayout(new BorderLayout());
        graphPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        graphPanel.add(graph);
        setLayout(new BorderLayout());
        add(graphPanel, BorderLayout.CENTER);
        add(settings, BorderLayout.SOUTH);
    }
}
