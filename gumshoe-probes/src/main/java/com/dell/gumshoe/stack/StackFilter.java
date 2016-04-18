package com.dell.gumshoe.stack;


public interface StackFilter {
    public int filter(StackTraceElement[] buffer, int size);

    public static StackFilter NONE = new StackFilter() {
        @Override
        public int filter(StackTraceElement[] buffer, int size) {
            return size;
        }
    };
}