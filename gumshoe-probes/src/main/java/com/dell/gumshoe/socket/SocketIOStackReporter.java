package com.dell.gumshoe.socket;

import com.dell.gumshoe.socket.SocketIOListener.DetailAccumulator;
import com.dell.gumshoe.socket.SocketIOMonitor.Event;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** create hierarchical analysis of socket IO versus stack
 *  with total IO at each frame and
 *  link from frame to multiple next frames below
 */
public class SocketIOStackReporter extends TimerTask {
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final SocketIOAccumulator source;

    public SocketIOStackReporter(SocketIOAccumulator source) {
        this.source = source;
    }

    @Override
    public void run() {
        final Map<Stack, DetailAccumulator> stats = new HashMap<>(source.getStats());
        for(Listener listener : listeners) {
            try {
                listener.socketIOStatsReported(stats);
            } catch(RuntimeException e) {
                e.printStackTrace();
            }

        }
        source.reset();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public static interface Listener {
        public void socketIOStatsReported(Map<Stack, DetailAccumulator> stats);
    }

    public static class StreamReporter implements Listener {
        private final PrintStream target;

        public StreamReporter(PrintStream target) {
            this.target = target;
        }

        @Override
        public void socketIOStatsReported(Map<Stack, DetailAccumulator> stats) {
            final StringBuilder out = new StringBuilder();
            addStartTag(out);
            addReport(out, stats);
            addEndTag(out);

            target.print(out.toString());
            target.flush();
        }

        protected void addStartTag(StringBuilder out) {
            final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            final String now = fmt.format(new Date());
            out.append("<diag type='socket-io' time='").append(now).append("'>\n");
        }

        private void addReport(StringBuilder out, Map<Stack, DetailAccumulator> stats) {
            for(Map.Entry<Stack, DetailAccumulator> entry : stats.entrySet()) {
                out.append(entry.getValue().get().toString()).append("\n").append(entry.getKey());
            }
        }

        protected void addEndTag(StringBuilder out) {
            out.append("</diag>\n");
        }
    }
}
