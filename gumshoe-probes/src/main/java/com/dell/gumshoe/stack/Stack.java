package com.dell.gumshoe.stack;

import java.util.Arrays;

public class Stack {
    public static final String FRAME_PREFIX = "    at ";
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
        return frames.clone();
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
            out.append(FRAME_PREFIX).append(frame).append("\n");
        }
        return out.toString();
    }

    public boolean isEmpty() {
        return frames.length==0;
    }

    public static StackTraceElement parseFrame(String line) {

        final String filename;
        final int lineNumber;
        if(line.endsWith("(Unknown Source)")) {
            filename = null;
            lineNumber = -1;
        } else if(line.endsWith("(Native Method)")) {
            filename = null;
            lineNumber = -2;
        } else if(line.contains(":")) {
            final String[] parts = line.split("[(:)]");
            filename = parts[1];
            lineNumber = Integer.parseInt(parts[2]);
        } else {
            final String[] parts = line.split("[(:)]");
            filename = parts[1];
            lineNumber = -1;
        }

        String[] parts = line.split("[ (]+");
        final String classAndMethod = parts[2];
        parts = classAndMethod.split("\\.");
        final String methodName = parts[parts.length-1];
        final String className = classAndMethod.substring(0, classAndMethod.length()-methodName.length()-1);

        return new StackTraceElement(className, methodName, filename, lineNumber);
    }
}
