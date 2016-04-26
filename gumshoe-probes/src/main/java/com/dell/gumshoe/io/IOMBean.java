package com.dell.gumshoe.io;


public interface IOMBean {
    public void setEnabled(boolean enabled);
    public boolean isEnabled();

    public String getReport();
    public void reset();

    public void setReportingFrequency(long millis);
    public long getReportingFrequency();

    public void setShutdownReportEnabled(boolean enabled);
    public boolean isShutdownReportEnabled();

    public void setHandlerThreadCount(int count);
    public int getHandlerThreadCount();

    public void setHandlerPriority(int value);
    public int getHandlerPriority();

    public int getEventQueueSize();

    public String getQueueStats();
    public void resetQueueCounters();

    public void setQueueStatisticsEnabled(boolean enabled);
    public boolean isQueueStatisticsEnabled();
}
