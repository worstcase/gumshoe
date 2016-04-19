package com.dell.gumshoe.io;

import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.StackStatisticSource;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** create hierarchical analysis of socket IO versus stack
 *  with total IO at each frame and
 *  link from frame to multiple next frames below
 */
public abstract class IOAccumulator<T extends IODetailAdder> implements IOListener, StackStatisticSource<T> {
    private ConcurrentMap<Stack,T> totals = new ConcurrentHashMap<>();
    private StackFilter filter;

    public IOAccumulator(StackFilter filter) {
        this.filter = filter;
    }

    public void setFilter(StackFilter filter) {
        this.filter = filter;
        totals = new ConcurrentHashMap<>();
    }

    @Override
    public void ioHasCompleted(IOEvent event) {
        Stack stack = event.getStack().applyFilter(filter);
        final T total = getAccumulator(stack);
        total.add(event);
    }

    private T getAccumulator(Stack key) {
        final ConcurrentMap<Stack,T> totalLocalCopy = this.totals;
        final T entry = totalLocalCopy.get(key);
        if(entry!=null) {
            return entry;
        }
        totalLocalCopy.putIfAbsent(key, newAdder());
        return totalLocalCopy.get(key);
    }

    public abstract T newAdder();

    /** THREAD SAFETY: the object returned may be modified while you are using it.
     *  callers should not keep this reference long,
     *  and an entry may continue to be modified after fetched.
     */
    @Override
    public Map<Stack,T> getStats() {
        return Collections.unmodifiableMap(totals);
    }

    @Override
    public void reset() {
        totals = new ConcurrentHashMap<>();
    }
}
