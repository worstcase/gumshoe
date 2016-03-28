package com.dell.gumshoe.stack;


public interface StackFilter {
    public int filter(StackTraceElement[] buffer, int size);
}