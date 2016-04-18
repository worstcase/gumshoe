package com.dell.gumshoe.tools;

import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class FileSourcePanel extends JPanel {
    private static final SimpleDateFormat hms = new SimpleDateFormat("HH:mm:ss");

    private FileDataParser file;
    private final StatisticsSourcePanel parent;

    private final JTextField fileNameField = new JTextField();
    private final JFileChooser fileChooser = new JFileChooser();
    private final JButton openButton = new JButton("Parse");

    public FileSourcePanel(final StatisticsSourcePanel parent) {
        this.parent = parent;
        fileChooser.setApproveButtonText("Parse");
        final JButton chooseButton = new JButton("File:");
        chooseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int result = fileChooser.showOpenDialog(FileSourcePanel.this);
                if(result==JFileChooser.APPROVE_OPTION) {
                    fileNameField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                    openFile();
                }
            }
        });
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });
        fileNameField.setColumns(30);

        final JPanel fileNamePanel = new JPanel();
        fileNamePanel.add(chooseButton);
        fileNamePanel.add(fileNameField);
        fileNamePanel.add(openButton);

        final JButton back = new JButton("<< Previous");
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readSample(false);
            }
        });
        final JButton forward = new JButton("Next >>");
        forward.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readSample(true);
            }
        });

        final JPanel actionPanel = new JPanel();
        actionPanel.add(back);
        actionPanel.add(forward);
        setLayout(new BorderLayout());
        add(fileNamePanel, BorderLayout.NORTH);
        add(actionPanel, BorderLayout.CENTER);
    }

    private void openFile() {
        if(file!=null) {
            try { file.close(); }
            catch(Exception ex) {
                ex.printStackTrace();
            }
            file = null;
        }
        try {
            final String fileName = fileNameField.getText();
            if("".equals(fileName.trim())) {
                parent.setStatus("No file selected");
            } else if( ! new File(fileName).canRead()) {
                parent.setStatus("Unable to read file");
            } else {
                file = new FileDataParser(fileName);
                openButton.setEnabled(false);
                parent.setStatus("Reading file: " + fileName);
                readSample(true);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private class FileOpener extends SwingWorker<Map<Stack,StatisticAdder>,Object> {
        private final boolean forward;
        public FileOpener(boolean forward) {
            this.forward = forward;
        }
        @Override
        public Map<Stack,StatisticAdder> doInBackground() throws Exception {
            return forward ? file.getNextSample() : file.getPreviousSample();
        }
        @Override
        public void done() {
            try {
                final Map<Stack,StatisticAdder> sample = get();
                if(sample==null) {
                    parent.setStatus("No more samples found in file");
                    parent.setSample(null);
                } else {
                    final Date time = file.getSampleTime();
                    parent.setStatus("File sample time: " + hms.format(time));
                    parent.setSample(sample);
                }
            } catch(Exception ex) {
                parent.setStatus("Parse error reading file");
                parent.setSample(null);
            } finally {
                openButton.setEnabled(true);
            }
        }
    }

    private void readSample(boolean forward) {
        new FileOpener(forward).execute();
    }
}
