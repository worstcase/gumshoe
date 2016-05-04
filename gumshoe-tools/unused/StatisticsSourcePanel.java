package com.dell.gumshoe.inspector;
import static com.dell.gumshoe.util.Swing.flow;
import static com.dell.gumshoe.util.Swing.groupButtons;
import static com.dell.gumshoe.util.Swing.stackNorth;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.inspector.tools.ProbeSourcePanel;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatisticsSourcePanel extends JPanel {
    static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");

    private final FileSourcePanel fileSource;
    private final ProbeSourcePanel probeSource;
    private final JPanel cardPanel = new JPanel();
    private final CardLayout sourceCardLayout = new CardLayout();
    private final JLabel status = new JLabel("No data currently displayed");

    public StatisticsSourcePanel(ProbeManager probe) {
        this(new FileSourcePanel(), new ProbeSourcePanel(probe));
    }

    public StatisticsSourcePanel(FileSourcePanel fileSource, ProbeSourcePanel probeSource) {
        this.fileSource = fileSource;
        this.probeSource = probeSource;
        fileSource.setParent(this);
//        probeSource.setParent(this);

        final JRadioButton jvmButton = new JRadioButton("probe this JVM");
        jvmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sourceCardLayout.show(cardPanel, "jvm");
            }
        });
        final JRadioButton fileButton = new JRadioButton("text file");
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sourceCardLayout.show(cardPanel, "file");
            }
        });

        groupButtons(jvmButton, fileButton);
        final JPanel sourcePanel = flow(new JLabel("Source:"), jvmButton, fileButton);

        cardPanel.setLayout(sourceCardLayout);
//        cardPanel.add(fileSource, "file");
//        cardPanel.add(probeSource, "jvm");

        setLayout(new BorderLayout());
        add(stackNorth(flow(status), sourcePanel, cardPanel), BorderLayout.NORTH);

        // if there is a main, that is the default source
//        if(probe!=null) {
            jvmButton.setSelected(true);
            sourceCardLayout.show(cardPanel, "jvm");
//        } else {
//            jvmButton.setEnabled(false);
//            fileButton.setSelected(true);
//            sourceCardLayout.show(cardPanel, "file");
//        }
    }

    public void setStatus(String message) {
        status.setText(message);
    }

    public void setSample(String time, Map<Stack,StatisticAdder> stats) {
        if(stats!=null) {
            final String type = stats.values().iterator().next().getType();
            notifyListeners(time, type, stats);
        } else {
            notifyListeners(null, null, null);
        }
    }

    /////

    private List<Listener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(String time, String type, Map<Stack,StatisticAdder> stats) {
        for(Listener listener : listeners) {
            listener.statisticsLoaded(time, type, stats);
        }
    }

    public interface Listener {
        public void statisticsLoaded(String time, String type, Map<Stack,StatisticAdder> stats);
    }
}
