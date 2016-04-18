package com.dell.gumshoe.thread;

import com.dell.gumshoe.stack.Stack;

public interface ThreadFilter {
    public static ThreadFilter NONE = new ThreadFilter() {
        @Override
        public boolean useThread(Stack unused, ThreadDetails ignored) { return true; }
    };

    public boolean useThread(Stack stack, ThreadDetails details);
}
