package com.dell.gumshoe.stack;

public class MinutiaFilter implements StackFilter {
    public enum Level {
        NO_LINE_NUMBERS {
            public StackTraceElement modify(StackTraceElement orig) {
                return new StackTraceElement(orig.getClassName(), orig.getMethodName(), orig.getFileName(), -1);
            }
        },

        NO_METHOD {
            public StackTraceElement modify(StackTraceElement orig) {
                return new StackTraceElement(orig.getClassName(), "", orig.getFileName(), -1);
            }
        },

        NO_INNER_CLASSES {
            public StackTraceElement modify(StackTraceElement orig) {
                final String fullName = orig.getClassName();
                final int index = fullName.indexOf('$');
                final String modifiedName = index>=0 ? fullName.substring(0, index-1) : fullName;
                return new StackTraceElement(modifiedName, "", orig.getFileName(), -1);
            }
        },
        NO_CLASSES {
            public StackTraceElement modify(StackTraceElement orig) {
                final String fullName = orig.getClassName();
                final int index = fullName.lastIndexOf('.');
                final String modifiedName = index>=0 ? fullName.substring(0, index-1) : fullName;
                return new StackTraceElement(modifiedName, "", null, -1);
            }
        };

        public abstract StackTraceElement modify(StackTraceElement orig);
    }

    private final Level level;

    public MinutiaFilter(String level) {
        this(Level.valueOf(level));
    }
    public MinutiaFilter(Level level) {
        this.level = level;
    }

    @Override
    public int filter(StackTraceElement[] buffer, int size) {
        for(int i=0;i<size;i++) {
            final StackTraceElement orig = buffer[i];
            buffer[i] = level.modify(orig);
        }
        return size;
    }



}
