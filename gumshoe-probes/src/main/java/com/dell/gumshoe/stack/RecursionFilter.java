package com.dell.gumshoe.stack;

public class RecursionFilter implements StackFilter {
    private final int depth;
    private final int threshold;

    public RecursionFilter(int depth, int threshold) {
        this.depth = depth;
        this.threshold = threshold;
    }

    @Override
    public int filter(StackTraceElement[] buffer, int size) {
        // only filter BIG stacks
        if(size<threshold) { return size; }

        int outSize = size;
        final int maxDepth = depth<1 ? size/2 : Math.min(depth, size/2);
        for(int sequenceLength=1;sequenceLength<=maxDepth;sequenceLength++) {
            for(int sourceIndex=0;sourceIndex<outSize-sequenceLength*2;sourceIndex++) {
                // source: buffer[sourceIndex]..buffer[sourceIndex+depth]
                // check source this matches the
                final int matchStart=sourceIndex+sequenceLength;
                final int matchEnd=getMatchEnd(buffer, sourceIndex, matchStart, sequenceLength);
                if(matchEnd>matchStart) {
                    // we have a 1+ matches of the source sequence
                    // target: buffer[matchStart]...buffer[matchEnd-1]
                    // so remove them from buffer and adjust the loop accordingly
                    final int remaining = outSize-matchEnd;
                    if(remaining>0) {
                        System.arraycopy(buffer, matchEnd, buffer, matchStart, remaining);
                    }
                    outSize -= matchEnd-matchStart;
                }
            }
        }
        return outSize;
    }

    private int getMatchEnd(StackTraceElement[] buffer, int source, int target, int length) {
        while(matches(buffer, source, target, length)) {
            target += length;
        }
        return target;
    }

    private boolean matches(StackTraceElement[] buffer, int source, int target, int length) {
        for(int i=0;i<length;i++) {
            if( ! buffer[source+i].equals(buffer[target+i])) {
                return false;
            }
        }
        return true;
    }
}
