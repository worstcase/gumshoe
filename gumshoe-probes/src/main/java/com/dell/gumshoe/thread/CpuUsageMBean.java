package com.dell.gumshoe.thread;


public interface CpuUsageMBean {
    public long getJitter();
    public void setJitter(long jitter);
    public long getDumpInterval();
    public void setDumpInterval(long dumpInterval);
    public long getEffectiveInterval();
    public long getAverageDumpTime();

    public int getThreadPriority();
    public void setThreadPriority(int threadPriority);


    public void setEnabled(boolean enabled);
    public boolean isEnabled();

    public void setReportingFrequency(long millis);
    public long getReportingFrequency();

    public void setShutdownReportEnabled(boolean enabled);
    public boolean isShutdownReportEnabled();

    public String getReport();
    public void reset();
}
