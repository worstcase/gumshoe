package com.dell.gumshoe.stack;

import java.util.ArrayList;
import java.util.List;

/** create custom filters
 *
 */
public class Filter implements StackFilter {
    public static StackFilter NONE = Filter.builder().build();

    public static Builder builder() { return new Builder(); }

    private final boolean withOriginal;
    private final List<StackFilter> filters;

    public Filter(boolean withOriginal, List<StackFilter> filters) {
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

    public static class Builder {
        private boolean withOriginal = false;
        private final List<StackFilter> filters = new ArrayList<>();

        private Builder() { }

        public Builder withFilter(StackFilter filter) {
            filters.add(filter);
            return this;
        }

        public Builder withOriginalIfBlank() {
            withOriginal = true;
            return this;
        }

        public Builder withExcludePlatform() {
            withExcludeClasses("sunw.");
            withExcludeClasses("sun.");
            withExcludeClasses("java.");
            withExcludeClasses("javax.");
            withExcludeClasses("com.dell.gumshoe.");
            return this;
        }

        public Builder withExcludeClasses(final String startingWith) {
            filters.add(new FrameMatcher() {
                @Override
                public boolean matches(StackTraceElement frame) {
                    return ! frame.getClassName().startsWith(startingWith);
                } });
            return this;
        }

        public Builder withOnlyClasses(final String startingWith) {
            filters.add(new FrameMatcher() {
                @Override
                public boolean matches(StackTraceElement frame) {
                    return frame.getClassName().startsWith(startingWith);
                }
            });
            return this;
        }

        public Builder withEndsOnly(final int topCount, final int bottomCount) {
            if(topCount==0 && bottomCount==0) { return this; }

            filters.add(new StackFilter() {
                @Override
                public int filter(StackTraceElement[] buffer, int size) {
                    final int maxSize = topCount + bottomCount;
                    final int frameCount = Math.min(size,  maxSize);
                    if(frameCount<size) {
                        // orig: [ 0 1 2 3 4 5 6 7 8 ]
                        // keep:   -----         ---    top=3, bottom=2
                        // copy: 7 --> 3 count 2
                        // out:  [ 0 1 2 7 8 ]
                        System.arraycopy(buffer, size-bottomCount, buffer, topCount, bottomCount);
                    }
                    return frameCount;
                }
            });
            return this;
        }

        public StackFilter build() {
            return new Filter(withOriginal, new ArrayList<>(filters));
        }
    }
}
