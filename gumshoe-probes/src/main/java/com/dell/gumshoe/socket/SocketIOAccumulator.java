package com.dell.gumshoe.socket;

import com.dell.gumshoe.socket.SocketIOMonitor.Event;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** create hierarchical analysis of socket IO versus stack
 *  with total IO at each frame and
 *  link from frame to multiple next frames below
 */
public class SocketIOAccumulator implements SocketIOListener {
    private final ConcurrentMap<Stack,DetailAccumulator> totals = new ConcurrentHashMap<>();
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
        final DetailAccumulator total = getAccumulator(stack);
        total.add(value);
    }

    private DetailAccumulator getAccumulator(Stack key) {
        final DetailAccumulator entry = totals.get(key);
        if(entry!=null) {
            return entry;
        }
        totals.putIfAbsent(key, new DetailAccumulator());
        return totals.get(key);
    }

    public Map<Stack,DetailAccumulator> getStats() {
        return totals;
    }

    public void reset() {
        totals.clear();
    }
}
