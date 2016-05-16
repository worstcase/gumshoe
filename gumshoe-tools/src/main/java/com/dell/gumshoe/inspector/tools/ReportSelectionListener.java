package com.dell.gumshoe.inspector.tools;

import com.dell.gumshoe.inspector.ReportSource;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import java.util.Map;

public interface ReportSelectionListener {
    public void reportWasSelected(Object source, String time, String type, Map<Stack,StatisticAdder> data);
    public void contentsChanged(ReportSource source);
}