package com.dell.gumshoe.util;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Scrollable;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;

/** utility methods to simplify layout and construction of swing GUI */
public class Swing {
    /** add these buttons to a new buttongroup (ie, make them mutually exclusive) */
    public static void groupButtons(AbstractButton... buttons) {
        final ButtonGroup group = new ButtonGroup();
        for(AbstractButton button : buttons) {
            group.add(button);
        }
    }

    /** add these components to a new panel, one in each column */
    public static JPanel columns(JComponent... components) {
        final JPanel out = new JPanel();
        out.setLayout(new GridLayout(1, components.length));
        for(JComponent component : components) {
            out.add(component);
        }
        return out;
    }

    public static JPanel rows(JComponent... components) {
        final JPanel out = new JPanel();
        out.setLayout(new GridLayout(components.length, 1));
        for(JComponent component : components) {
            out.add(component);
        }
        return out;
    }

    /** add these components to a new panel, one in each column */
    public static JPanel grid(int cols, JComponent... components) {
        final JPanel out = new JPanel();
        out.setLayout(new GridLayout(components.length/cols+1, cols));
        for(JComponent component : components) {
            out.add(component);
        }
        return out;
    }

    /** add these components to a new panel, stacking along the left side */
    public static JPanel stackWest(JComponent... components) {
        return stack(BorderLayout.WEST, components);
    }

    /** add these components to a new panel, stacking along the top side */
    public static JPanel stackNorth(JComponent... components) {
        return stack(BorderLayout.NORTH, components);
    }

    /** add these components to a new panel, stacking along the top side */
    public static JPanel stackSouth(JComponent... components) {
        return stack(BorderLayout.SOUTH, components);
    }

    /** add these components to a new panel, stacking along the given side */
    public static JPanel stack(String edge, JComponent... components) {
        return stackIn(new JPanel(new BorderLayout()), edge, components);
    }

    /** add these components to a new panel, stacking along the given side */
    public static JPanel stackIn(JPanel container, String edge, JComponent... components) {
        JPanel inner = container;
        for(JComponent component : components) {
            final JPanel newInner = new JPanel(new BorderLayout());
            newInner.add(component, edge);
            inner.add(newInner, BorderLayout.CENTER);
            inner = newInner;
        }
        return container;
    }

    public static JPanel flow(JComponent... components) {
        final JPanel out = new JPanel();
        for(JComponent component : components) {
            out.add(component);
        }
        return out;
    }

    public static JPanel titled(String title, JComponent content) {
        final Border lineBorder = BorderFactory.createLineBorder(Color.black);
        final TitledBorder titledBorder = BorderFactory.createTitledBorder(lineBorder, title, TitledBorder.LEFT, TitledBorder.TOP);
        if(content instanceof JPanel) {
            content.setBorder(titledBorder);
            return (JPanel) content;
        } else {
            final JPanel out = new JPanel();
            out.setBorder(titledBorder);
            out.setLayout(new BorderLayout());
            out.add(content, BorderLayout.CENTER);
            return out;
        }
    }

    public static JTabbedPane createTabbedPaneWithoutHScroll() {
        return new JTabbedWithoutHScroll();
    }

    // track down layout issues by showing following size through container hierarchy
    public static void debugWidth(Component c) {
        int lastWidth = c.getWidth();
        while(c!=null) {
            final String className = c.getClass().getName();
            final String[] parts = className.split("\\.");
            final String lastPart = parts[parts.length-1];
            int thisWidth = c.getWidth();
            final float percent = ((float)Math.abs(thisWidth-lastWidth))/lastWidth;
            boolean delta = percent > .1;
            if(delta) {
                System.out.printf(">> %25s %5d\n", lastPart, c.getWidth());
            } else {
                System.out.printf("%28s %5d %.2f\n", lastPart, c.getWidth(), percent);
            }
            c = c.getParent();
            lastWidth = thisWidth;
        }
        System.out.println("-----");
    }

    private static class JTabbedWithoutHScroll extends JTabbedPane implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 1;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
