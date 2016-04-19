package com.dell.gumshoe.tools;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

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
        JPanel out = null;
        JPanel inner = null;
        for(JComponent component : components) {
            if(out==null) {
                out = inner = new JPanel();
            } else {
                final JPanel newInner = new JPanel();
                inner.add(newInner, BorderLayout.CENTER);
                inner = newInner;
            }
            inner.setLayout(new BorderLayout());
            inner.add(component, edge);
        }
        return out;
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
}
