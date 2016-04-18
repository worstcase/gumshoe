package com.dell.gumshoe.socket.io;

import com.dell.gumshoe.socket.io.SocketIOMonitor.Event;
import com.dell.gumshoe.socket.io.SocketIOMonitor.SocketIOListener;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.StackStatisticSource;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** create hierarchical analysis of socket IO versus stack
 *  with total IO at each frame and
 *  link from frame to multiple next frames below
 */
public class SocketIOAccumulator implements SocketIOListener, StackStatisticSource<SocketIODetailAdder> {
    private ConcurrentMap<Stack,SocketIODetailAdder> totals = new ConcurrentHashMap<>();
    private StackFilter filter;

    public SocketIOAccumulator(StackFilter filter) {
        this.filter = filter;
    }

    public void setFilter(StackFilter filter) {
        this.filter = filter;
        totals = new ConcurrentHashMap<>();
    }

    @Override
    public void socketIOHasCompleted(Event event) {
        final IODetail value = new IODetail(event);
        Stack stack = event.getStack().applyFilter(filter);
        final SocketIODetailAdder total = getAccumulator(stack);
        total.add(value);
    }

    private SocketIODetailAdder getAccumulator(Stack key) {
        final ConcurrentMap<Stack,SocketIODetailAdder> totalLocalCopy = this.totals;
        final SocketIODetailAdder entry = totalLocalCopy.get(key);
        if(entry!=null) {
            return entry;
        }
        totalLocalCopy.putIfAbsent(key, new SocketIODetailAdder());
        return totalLocalCopy.get(key);
    }

    /** THREAD SAFETY: the object returned may be modified while you are using it.
     *  callers should not keep this reference long,
     *  and an entry may continue to be modified after fetched.
     */
    @Override
    public Map<Stack,SocketIODetailAdder> getStats() {
        return Collections.unmodifiableMap(totals);
    }

    @Override
    public void reset() {
        totals = new ConcurrentHashMap<>();
    }
}
