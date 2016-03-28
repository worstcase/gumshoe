package com.dell.gumshoe.socket;

import com.dell.gumshoe.socket.SocketIOListener.DetailAccumulator;
import com.dell.gumshoe.stack.Stack;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/** create hierarchical analysis of socket IO versus stack
 *  with total IO at each frame and
 *  link from frame to multiple next frames below
 */
public class SocketIOStackReporter extends TimerTask {
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final MessageFormat START_TAG_PATTERN = new MessageFormat("<gumshoe-report type=''socket-io'' time=''{0}''>");
    public static final String END_TAG = "</gumshoe-report>";
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
            final String now = formatDate(new Date());
            out.append(formatStartTag(now)).append("\n");
        }

        private void addReport(StringBuilder out, Map<Stack, DetailAccumulator> stats) {
            for(Map.Entry<Stack, DetailAccumulator> entry : stats.entrySet()) {
                out.append(entry.getValue().get().toString()).append("\n").append(entry.getKey());
            }
        }

        protected void addEndTag(StringBuilder out) {
            out.append(END_TAG).append("\n");
        }
    }

    private static String formatDate(Date d) {
        synchronized(DATE_TIME_FORMAT) {
            return DATE_TIME_FORMAT.format(d);
        }
    }

    private static Date parseDate(String date) throws ParseException {
        synchronized(DATE_TIME_FORMAT) {
            return DATE_TIME_FORMAT.parse(date);
        }
    }

    private static String formatStartTag(String date) {
        synchronized(START_TAG_PATTERN) {
            return START_TAG_PATTERN.format(new Object[] { date });
        }
    }

    public static Date parseStartTag(String line) {
        try {
            final String dateField;
            synchronized(START_TAG_PATTERN) {
                final Object[] fields = START_TAG_PATTERN.parse(line);
                dateField = (String) fields[0];
            }
            return parseDate(dateField);
        } catch(Exception e) {
            return null;
        }
    }

    public static boolean isEndTag(String line) {
        return END_TAG.equals(line);
    }
}
