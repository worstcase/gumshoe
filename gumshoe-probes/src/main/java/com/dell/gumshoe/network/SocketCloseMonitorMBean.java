package com.dell.gumshoe.network;


public interface SocketCloseMonitorMBean {
    public void setEnabled(boolean enabled);
    public boolean isEnabled();
    public String getReport(long age);
    public void setReportingFrequency(long millis);
    public long getReportingFrequency();
    public void setClearClosedSocketsInterval(int numberOfSockets);
    public int getClearClosedSocketsInterval();
    public void setShutdownReportEnabled(boolean enabled);
    public boolean isShutdownReportEnabled();
}
