package com.dell.gumshoe.network;


public interface SocketIOMBean {
    public void setEnabled(boolean enabled);
    public boolean isEnabled();

    public String getReport();
    public void reset();

    public void setReportingFrequency(long millis);
    public long getReportingFrequency();

    public void setShutdownReportEnabled(boolean enabled);
    public boolean isShutdownReportEnabled();
}
