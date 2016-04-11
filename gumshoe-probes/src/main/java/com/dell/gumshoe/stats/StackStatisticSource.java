package com.dell.gumshoe.stats;

import com.dell.gumshoe.stack.Stack;

import java.util.Map;

public interface StackStatisticSource<T extends StatisticAdder> {
    public Map<Stack,T> getStats();
    public void reset();
}
