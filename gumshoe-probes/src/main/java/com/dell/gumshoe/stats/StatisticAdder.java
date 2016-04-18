package com.dell.gumshoe.stats;


public interface StatisticAdder<V> {
    public void add(StatisticAdder<V> value);
    public void add(V value);
    public StatisticAdder<V> newInstance();
    public String getType();
}