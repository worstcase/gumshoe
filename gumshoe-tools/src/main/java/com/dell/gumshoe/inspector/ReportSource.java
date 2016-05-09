package com.dell.gumshoe.inspector;

import com.dell.gumshoe.inspector.tools.ReportSelectionListener;

public interface ReportSource {
    public void addListener(ReportSelectionListener listener);
    public boolean hasNext();
    public boolean hasPrevious();
    public void nextReport();
    public void previousReport();
}
