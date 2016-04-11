package com.dell.gumshoe.socket.unclosed;


public interface SocketCloseMonitorMBean {
    public void setEnabled(boolean enabled);
    public boolean isEnabled();
    public String getReport(long age);
}
