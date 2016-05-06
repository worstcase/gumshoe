package com.dell.gumshoe.inspector.tools;

import static com.dell.gumshoe.util.Swing.flow;
import static com.dell.gumshoe.util.Swing.stackIn;
import static com.dell.gumshoe.util.Swing.stackNorth;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionListener;

public class AboutPanel extends JPanel implements HasCloseButton {
    private final JButton ok = new JButton("OK");

    public AboutPanel() {
        super(new BorderLayout());

        final Font bold = getFont().deriveFont(Font.BOLD);
        final JLabel gumshoe = new JLabel("Gumshoe");
        final JLabel investigator = new JLabel("Inspector");
        gumshoe.setFont(bold);
        investigator.setFont(bold);

        stackIn(this, BorderLayout.NORTH,
                flow(investigator,
                     new JLabel("GUI for viewing Gumshoe results")),
                flow(new JLabel("is part of")),
                flow(gumshoe,
                     new JLabel("resource utilization and performance analysis tools")),
                flow(new JLabel("(c) 2016 Dell, Inc.")),
                flow(new JLabel("Use allowed under terms of Apache License 2.0.")),
                flow(new JLabel("")),
                stackNorth(new JLabel("Thanks for the encouragement and support from the project managers", SwingConstants.LEFT)),
                stackNorth(new JLabel("at Dell Software Group who saw the value and set aside the time", SwingConstants.LEFT)),
                stackNorth(new JLabel("to develop this tool and share it with our customers and community.", SwingConstants.LEFT)),
                flow(new JLabel("")),
                flow(ok) );
        setBorder(BorderFactory.createEmptyBorder(10,15,7,15));
    }

    @Override
    public void addCloseListener(ActionListener listener) {
        ok.addActionListener(listener);
    }
}
