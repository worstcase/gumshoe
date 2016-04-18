package com.dell.gumshoe.thread;


public interface CpuUsageMBean {
    public long getDumpInterval();
    public void setDumpInterval(long dumpInterval);
    public int getThreadPriority();
    public void setThreadPriority(int threadPriority);
    public long getEffectiveInterval();
    public void setEnabled(boolean enabled);
    public boolean isEnabled();
    public void reset();
    public long getAverageDumpTime();
}
