package com.dell.gumshoe.stats;


public interface StatisticAccumulator<V> {
    public void add(StatisticAccumulator<V> value);
    public void add(V value);
    public V get();
    public StatisticAccumulator<V> newInstance();
}