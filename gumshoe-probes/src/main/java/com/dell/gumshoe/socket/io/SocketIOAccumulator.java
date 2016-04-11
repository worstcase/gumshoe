package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.socket.io.SocketIOMonitor.Event;
import com.dell.gumshoe.socket.io.SocketIOMonitor.SocketIOListener;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.StackStatisticSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** create hierarchical analysis of socket IO versus stack
 *  with total IO at each frame and
 *  link from frame to multiple next frames below
 */
public class SocketIOAccumulator implements SocketIOListener, StackStatisticSource {
    private final ConcurrentMap<Stack,IODetailAccumulator> totals = new ConcurrentHashMap<>();
    private StackFilter filter;

    public SocketIOAccumulator(StackFilter filter) {
        this.filter = filter;
    }

    public void setFilter(StackFilter filter) {
        this.filter = filter;
        totals.clear();
    }

    @Override
    public void socketIOHasCompleted(Event event) {
        final IODetail value = new IODetail(event);
        Stack stack = event.getStack().applyFilter(filter);
        final IODetailAccumulator total = getAccumulator(stack);
        total.add(value);
    }

    private IODetailAccumulator getAccumulator(Stack key) {
        final IODetailAccumulator entry = totals.get(key);
        if(entry!=null) {
            return entry;
        }
        totals.putIfAbsent(key, new IODetailAccumulator());
        return totals.get(key);
    }

    @Override
    public Map<Stack,IODetailAccumulator> getStats() {
        return totals;
    }

    @Override
    public void reset() {
        totals.clear();
    }
}
