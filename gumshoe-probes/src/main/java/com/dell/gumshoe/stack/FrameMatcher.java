package com.dell.gumshoe.stack;


public abstract class FrameMatcher implements StackFilter {
    public abstract boolean matches(StackTraceElement frame);
    
    @Override
    public int filter(StackTraceElement[] mutable, int size) {
        int newSize = 0;
        for(int index=0;index<size;index++) {
            final StackTraceElement frame = mutable[index];
            if(matches(frame)) {
                if(newSize<index) {
                    mutable[newSize] = mutable[index];
                }
                newSize++;
            }
        }
        return newSize;
    }
}