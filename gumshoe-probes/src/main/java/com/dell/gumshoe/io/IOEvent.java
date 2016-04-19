package com.dell.gumshoe.io;

import com.dell.gumshoe.stack.Stack;

public abstract class IOEvent {
    private final Stack stack;
    private long elapsed;

    private String target;
    private long bytes;

    protected IOEvent() {
        elapsed = System.currentTimeMillis();
        stack = new Stack();
    }

    public void complete(long bytes) {
        elapsed = System.currentTimeMillis() - elapsed;
        this.bytes = bytes;
    }

    protected void setTarget(String target) { this.target = target; }

    public Stack getStack() { return stack; }
    public String getTarget() { return target; }
    public long getElapsed() { return elapsed; }
    public long getBytes() { return bytes; }
    public abstract boolean isRead();
    public abstract boolean isWrite();
}