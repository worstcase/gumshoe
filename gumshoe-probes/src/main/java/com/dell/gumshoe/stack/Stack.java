package com.dell.gumshoe.stack;

import java.util.Arrays;

public class Stack {
    private final StackTraceElement[] frames;

    public Stack() {
        this(new Throwable().getStackTrace());
    }

    public Stack(StackTraceElement[] frames) {
        this.frames = frames;
    }

    public Stack applyFilter(StackFilter filter) {
        final StackTraceElement[] buffer = frames.clone();
        final int size = filter.filter(buffer, frames.length);
        final StackTraceElement[] filteredStack = new StackTraceElement[size];
        if(size>0) {
            System.arraycopy(buffer, 0, filteredStack, 0, size);
        }
        return new Stack(filteredStack);
    }

    public StackTraceElement[] getFrames() {
        return frames;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(frames);
    }

    @Override
    public boolean equals(Object obj) {
        if( ! (obj instanceof Stack)) return false;
        Stack that = (Stack)obj;
        return Arrays.equals(this.frames, that.frames);
    }

    @Override
    public String toString() {
        final StringBuilder out = new StringBuilder();
        for(StackTraceElement frame : frames) {
            out.append("    at ").append(frame).append("\n");
        }
        return out.toString();
    }

    public boolean isEmpty() {
        return frames.length==0;
    }

}
