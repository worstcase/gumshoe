package com.dell.gumshoe.inspector.graph;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

public class Ruler extends JLabel {
    // pixel sizes
    public static final int RULER_HEIGHT = 20;
    private static final int RULER_MAJOR_HEIGHT = 20;
    private static final int RULER_MINOR_HEIGHT = 10;

    // number of major and minor divisions
    private static final int RULER_MAJOR = 4;
    private static final int RULER_MINOR = 20;

    private final JScrollPane scrollPane;
    public Ruler(JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    public Dimension getPreferredSize() {
        final Dimension size = scrollPane.getViewport().getView().getSize();
        size.height = RULER_HEIGHT;
        return size;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        final int width = getSize().width; // 100%
        final int height = getSize().height;
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, width, height);
        for(int i=1;i<RULER_MINOR;i++) {
            final int x = (width-1)*i/RULER_MINOR;
            g.drawLine(x, RULER_HEIGHT-RULER_MINOR_HEIGHT, x, RULER_HEIGHT);
        }

        for(int i=0;i<=RULER_MAJOR;i++) {
            final int x = (width-1)*i/RULER_MAJOR;
            g.drawLine(x, RULER_HEIGHT-RULER_MAJOR_HEIGHT, x, RULER_HEIGHT);
            g.drawLine(x+1, RULER_HEIGHT-RULER_MAJOR_HEIGHT, x+1, RULER_HEIGHT);
        }
      }
}
