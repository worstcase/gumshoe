package com.dell.gumshoe.stack;

import java.util.ArrayList;
import java.util.List;

public class StandardFilter {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final List<StackFilter> filters = new ArrayList<>();
        private final List<String> includePatterns = new ArrayList<>();
        private final List<String> excludePatterns = new ArrayList<>();

        private boolean withOriginal = false;
        private StackFilter endFilter;
        private String frameMinutia;
        private Integer recursionDepth;
        private int recursionThreshold;

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
            excludePatterns.add(startingWith);
            return this;
        }

        public Builder withOnlyClasses(final String startingWith) {
            includePatterns.add(startingWith);
            return this;
        }

        public Builder withEndsOnly(final int topCount, final int bottomCount) {
            if(topCount==0 && bottomCount==0) { return this; }

            endFilter = new StackFilter() {
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
            };
            return this;
        }

        public void withSimpleFrames(MinutiaFilter.Level level) {
            this.frameMinutia = level.toString();
        }

        public void withSimpleFrames(String simplifyLevel) {
            this.frameMinutia = simplifyLevel;
        }

        public void withRecursionFilter(int depth, int threshold) {
            recursionDepth = depth;
            recursionThreshold = threshold;
        }
        public StackFilter build() {
            if( ! excludePatterns.isEmpty()) {
                filters.add(new FrameMatcher() {
                    @Override
                    public boolean matches(StackTraceElement frame) {
                        final String className = frame.getClassName();
                        for(String pattern : excludePatterns) {
                            if(className.startsWith(pattern)) { return false; }
                        }
                        return true;
                    }
                });
            }
            if( ! includePatterns.isEmpty()) {
                filters.add(new FrameMatcher() {
                    @Override
                    public boolean matches(StackTraceElement frame) {
                        final String className = frame.getClassName();
                        for(String pattern : includePatterns) {
                            if(className.startsWith(pattern)) { return true; }
                        }
                        return false;
                    }
                });
            }
            if(endFilter!=null) {
                filters.add(endFilter);
            }

            if(frameMinutia!=null && ! "NONE".equals(frameMinutia)) {
                filters.add(new MinutiaFilter(frameMinutia));
            }

            if(recursionDepth!=null) {
                filters.add(new RecursionFilter(recursionDepth, recursionThreshold));
            }

            return new FilterSequence(withOriginal, new ArrayList<>(filters));
        }
    }

}
