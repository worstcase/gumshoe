package com.dell.gumshoe.inspector.tools;

import static com.dell.gumshoe.util.Swing.flow;
import static com.dell.gumshoe.util.Swing.stackIn;
import static com.dell.gumshoe.util.Swing.stackWest;

import com.dell.gumshoe.inspector.ReportSource;
import com.dell.gumshoe.inspector.graph.StackGraphPanel;
import com.dell.gumshoe.inspector.helper.DataTypeHelper;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class StatisticChooser extends JPanel implements ReportSelectionListener, HasCloseButton {
    private final JComboBox statSelector = new JComboBox(DataTypeHelper.getTypes().toArray());
    private final CardLayout statCard = new CardLayout();
    private final JPanel statOptions = new JPanel();
    private String lastLoadedType;
    private boolean updateWhenLoaded = true;
    private JButton close = new JButton("OK");
    private StackGraphPanel graph;

    public StatisticChooser(StackGraphPanel graph) {
        super(new BorderLayout());

        this.graph = graph;
        statSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String label = (String)statSelector.getSelectedItem();
                statCard.show(statOptions, label);
                // if user manually selects current displayed type,
                // keep them in sync if new type is loaded
                // otherwise leave it on user's selected type
                // so viewing stats live won't change dropdown while user choosing a stat
                updateWhenLoaded = label.equals(lastLoadedType);
            }
        });
        final JPanel statChooserPanel = stackWest(new JLabel("Select statistic for report type "), statSelector, new JLabel(":"));
        statOptions.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        statOptions.setLayout(statCard);
        for(String typeName : DataTypeHelper.getTypes()) {
            statOptions.add(DataTypeHelper.forType(typeName).getOptionEditor(), typeName);
        }

        stackIn(this, BorderLayout.NORTH, statChooserPanel, statOptions, flow(close));
    }

    @Override
    public void reportWasSelected(Object source, String time, String type, Map<Stack, StatisticAdder> data) {
        lastLoadedType = type;
        if( ! isShowing()) { updateWhenLoaded = true; }
        if(updateWhenLoaded) {
            statSelector.setSelectedItem(type);
        }
    }

    @Override
    public void addCloseListener(ActionListener listener) {
        close.addActionListener(listener);
        graph.repaint();
    }

    @Override
    public void contentsChanged(ReportSource source) {
        // no-op
    }
}
