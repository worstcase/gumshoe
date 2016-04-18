package com.dell.gumshoe.stats;

import com.dell.gumshoe.stack.Stack;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/** create hierarchical analysis of socket IO versus stack
 *  with total IO at each frame and
 *  link from frame to multiple next frames below
 */
public class ValueReporter<A extends StatisticAdder> {
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final MessageFormat START_TAG_PATTERN = new MessageFormat("<gumshoe-report type=''{0}'' time=''{1}''>");
    public static final String END_TAG = "</gumshoe-report>";
    private final List<Listener<A>> listeners = new CopyOnWriteArrayList<>();
    private final StackStatisticSource<A> source;
    private final String type;
    private final Task shutdownHook;
    private Task timerTask;
    private long timerFrequency;

    public ValueReporter(String type) {
        this(type, null);
    }

    public ValueReporter(String type, StackStatisticSource<A> source) {
        this.type = type;
        this.source = source;
        this.shutdownHook = new Task();
    }

    public static interface Listener<A extends StatisticAdder> {
        public void statsReported(String type, Map<Stack,A> stats);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void addStreamReporter(PrintStream out) {
        addListener(new StreamReporter(out));
    }

    public Runnable getShutdownHook() {
        return shutdownHook;
    }

    public void scheduleReportTimer(Timer timer, long frequency) {
        if(timerTask!=null) {
            cancelReportTimer();
        }
        timerTask = new Task();
        timer.scheduleAtFixedRate(timerTask, frequency, frequency);
        this.timerFrequency = frequency;
    }

    public void cancelReportTimer() {
        if(timerTask==null) {
            return;
        }
        timerTask.cancel();
        timerTask = null;
    }

    public long getReportTimerFrequency() {
        return timerFrequency;
    }

    private class Task extends TimerTask {
        @Override
        public void run() {
            final Map<Stack, A> stats = new HashMap<>(source.getStats());
            for(Listener listener : listeners) {
                try {
                    listener.statsReported(type, stats);
                } catch(RuntimeException e) {
                    e.printStackTrace();
                }

            }
            source.reset();
        }
    }

    public class StreamReporter implements Listener<A> {
        private final PrintStream target;

        public StreamReporter(PrintStream target) {
            this.target = target;
        }

        @Override
        public void statsReported(String type, Map<Stack,A> stats) {
            if( ! type.equals(ValueReporter.this.type)) { throw new IllegalArgumentException(); }
            statsReported(stats);
        }

        public void statsReported(Map<Stack,A> stats) {
            final StringBuilder out = new StringBuilder();
            addStartTag(out, ValueReporter.this.type);
            addReport(out, stats);
            addEndTag(out);

            target.print(out.toString());
            target.flush();
        }

        private void addStartTag(StringBuilder out, String type) {
            final String now = formatDate(new Date());
            out.append(formatStartTag(type, now)).append("\n");
        }

        private void addReport(StringBuilder out, Map<Stack,A> stats) {
            for(Map.Entry<Stack,A> entry : stats.entrySet()) {
                out.append(entry.getValue().toString()).append("\n").append(entry.getKey());
            }
        }

        private void addEndTag(StringBuilder out) {
            out.append(END_TAG).append("\n");
        }
    }

    private String formatDate(Date d) {
        synchronized(DATE_TIME_FORMAT) {
            return DATE_TIME_FORMAT.format(d);
        }
    }

    private static Date parseDate(String date) throws ParseException {
        synchronized(DATE_TIME_FORMAT) {
            return DATE_TIME_FORMAT.parse(date);
        }
    }

    private String formatStartTag(String type, String date) {
        synchronized(START_TAG_PATTERN) {
            return START_TAG_PATTERN.format(new Object[] { type, date });
        }
    }

    public static String parseStartTagType(String line) {
        try {
            synchronized(START_TAG_PATTERN) {
                final Object[] fields = START_TAG_PATTERN.parse(line);
                return (String)fields[0];
            }
        } catch(Exception e) {
            return null;
        }
    }

    public static Date parseStartTagTime(String line) {
        try {
            final String dateField;
            synchronized(START_TAG_PATTERN) {
                final Object[] fields = START_TAG_PATTERN.parse(line);
                dateField = (String) fields[1];
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
