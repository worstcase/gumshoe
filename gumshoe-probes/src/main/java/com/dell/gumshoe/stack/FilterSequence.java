package com.dell.gumshoe.stack;

import java.util.List;

/** create custom filters
 *
 */
public class FilterSequence implements StackFilter {
    private final boolean withOriginal;
    private final List<StackFilter> filters;

    public FilterSequence(boolean withOriginal, List<StackFilter> filters) {
        this.withOriginal = withOriginal;
        this.filters = filters;
    }

    @Override
    public int filter(StackTraceElement[] mutable, int origSize) {
        final StackTraceElement[] orig = withOriginal ? mutable.clone() : null;
        int size = origSize;
        for(StackFilter filter : filters) {
            size = filter.filter(mutable, size);
            if(size==0) break;
        }
        if(size==0 && withOriginal) {
            System.arraycopy(orig, 0, mutable, 0, origSize);
            return origSize;
        }
        return size;
    }

}
