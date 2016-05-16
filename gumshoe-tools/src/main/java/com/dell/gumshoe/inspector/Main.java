package com.dell.gumshoe.inspector;

import com.dell.gumshoe.Agent;
import com.dell.gumshoe.ProbeManager;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** inspector main window and launcher
 *
 *  running as an application,
 *  this will start inspector GUI in a frame and then look for arguments.
 *  first argument is the name of the target main class to run,
 *  remaining arguments are passed as the args to that main.
 */
public class Main {
    private static final int DEFAULT_WIDTH_PERCENT = 70;
    private static final int DEFAULT_HEIGHT_PERCENT = 90;

    public static void main(String[] args) throws Throwable {
        final boolean hasTargetApp = args.length>0;
        if(hasTargetApp) {
            launchTargetApp(args);
        }
        launchGUI(hasTargetApp);
    }

    private static void launchGUI(boolean hasMain) throws Exception {
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
        final boolean needProbe = forceProbe || hasMain;

        final ProbeManager probe = needProbe ? ProbeManager.getInstance() : null;

        final JFrame frame = new JFrame("Inspector");
        if( ! hasMain) {
            // if this is the only program, click X to exit (otherwise just hide)
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }

        frame.getContentPane().add(new GUI(frame, probe, hasMain));
        frame.pack();

        final Dimension fullScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int height = fullScreenSize.height;
        final int width = fullScreenSize.width;
        final Dimension unmaximizedSize = new Dimension(DEFAULT_WIDTH_PERCENT*width/100, DEFAULT_HEIGHT_PERCENT*height/100);
        frame.setSize(unmaximizedSize);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    private static void launchTargetApp(String[] args) throws Throwable {
        final String mainClassName = args[0];
        final Class mainClass = Class.forName(mainClassName);
        final Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);

        final String[] remainingArgs = new String[args.length-1];
        if(args.length>1) {
            System.arraycopy(args, 1, remainingArgs, 0, args.length-1);
        }

        try {
            mainMethod.invoke(mainClass, new Object[] { remainingArgs });
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
