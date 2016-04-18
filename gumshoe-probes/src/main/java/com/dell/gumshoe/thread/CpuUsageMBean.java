package com.dell.gumshoe.thread;

import com.dell.gumshoe.stack.StackFilter;

public interface CpuUsageMBean {
    public long getDumpInterval();
    public void setDumpInterval(long dumpInterval);
    public int getThreadPriority();
    public void setThreadPriority(int threadPriority);
    public long getEffectiveInterval();
    public void setEnabled(boolean enabled);
    public boolean isEnabled();
    public void reset();
}
