package com.dell.gumshoe.stats;

import com.dell.gumshoe.stack.Stack;

import java.util.Map;

public interface StackStatisticSource<T extends StatisticAccumulator> {
    public Map<Stack,T> getStats();
    public void reset();
}
