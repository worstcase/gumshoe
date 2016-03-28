package com.dell.gumshoe.tools;

import com.dell.gumshoe.Probe;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Gumshoe extends JPanel {
    public static void main(String[] args) throws Throwable {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }

        Probe probe = new Probe();
        probe.initialize();

        JFrame frame = new JFrame();
        frame.getContentPane().add(new Gumshoe(probe));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800,600);
        frame.setVisible(true);

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

    private Gumshoe(Probe probe) {
        final FlameGraph graph = new FlameGraph();
        final StatisticsRelay statsRelay = new StatisticsRelay(graph);
        probe.getIOReporter().addListener(statsRelay);

        final JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new BorderLayout());
        detailPanel.add(graph.getDetailField(), BorderLayout.CENTER);

        final JPanel statsPanel = new JPanel();
        statsPanel.add(statsRelay);

        final FilterEditor filterEditor = new FilterEditor();
        filterEditor.setGraph(graph);
        filterEditor.setProbe(probe);

        final JTabbedPane settings = new JTabbedPane();
        settings.setBorder(BorderFactory.createEmptyBorder(10,5,5,5));
        settings.addTab("Collect -->", statsPanel);
        settings.addTab("--> Filter -->", filterEditor);
        settings.addTab("--> Display", graph.getOptionEditor());
        settings.addTab("Examine", detailPanel);

        final JPanel graphPanel = new JPanel();
        graphPanel.setLayout(new BorderLayout());
        graphPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        graphPanel.add(graph);
        setLayout(new BorderLayout());
        add(graphPanel, BorderLayout.CENTER);
        add(settings, BorderLayout.SOUTH);
    }
}
