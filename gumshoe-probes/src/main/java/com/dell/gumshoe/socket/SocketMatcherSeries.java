package com.dell.gumshoe.socket;

public class SocketMatcherSeries implements SocketMatcher {
    private final SocketMatcher[] acceptList;
    private final SocketMatcher[] rejectList;
    
    public SocketMatcherSeries(SocketMatcher[] acceptList, SocketMatcher[] rejectList) {
        this.acceptList = acceptList;
        this.rejectList = rejectList;
    }

    @Override
    public boolean matches(byte[] addressBytes, int port) {
        for(SocketMatcher accept : acceptList) {
            if(accept.matches(addressBytes, port)) {
                return true;
            }
        }
        
        for(SocketMatcher reject : rejectList) {
            if(reject.matches(addressBytes, port)) {
                return false;
            }
        }
        
        return true;
    }
    
}
