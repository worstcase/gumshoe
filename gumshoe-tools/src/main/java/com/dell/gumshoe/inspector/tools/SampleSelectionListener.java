package com.dell.gumshoe.inspector.tools;

import com.dell.gumshoe.inspector.SampleSource;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import javax.swing.event.ChangeEvent;

import java.util.Map;

public interface SampleSelectionListener {
    public void sampleWasSelected(Object source, String time, String type, Map<Stack,StatisticAdder> data);
    public void contentsChanged(SampleSource source);
}