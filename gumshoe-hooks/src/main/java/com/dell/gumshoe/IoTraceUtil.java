package com.dell.gumshoe;

import sun.misc.IoTrace;

import java.lang.reflect.Field;

public class IoTraceUtil {

    public static void addTrace(IoTraceDelegate delegate) throws Exception {
        final Field nullField = IoTrace.class.getDeclaredField("NULL_OBJECT");
        nullField.setAccessible(true);
        final Object nullObject = nullField.get(IoTrace.class);

        final Field delegateField = IoTrace.class.getDeclaredField("delegate");
        delegateField.setAccessible(true);
        final Object oldValue = delegateField.get(IoTrace.class);
        if(oldValue==nullObject) {
            delegateField.set(IoTrace.class, delegate);
        } else if(oldValue instanceof IoTraceMultiplexer) {
            final IoTraceMultiplexer multi = (IoTraceMultiplexer) oldValue;
            multi.addDelegate(delegate);
        } else {
            final IoTraceMultiplexer multi = new IoTraceMultiplexer();
            multi.addDelegate((IoTraceDelegate)oldValue);
            delegateField.set(IoTrace.class, multi);
        }
    }

    public static void removeTrace(IoTraceDelegate delegate) throws Exception {
        final Field delegateField = IoTrace.class.getDeclaredField("delegate");
        delegateField.setAccessible(true);
        final Object oldValue = delegateField.get(IoTrace.class);
        if(oldValue.equals(delegate)) {
            final Field nullField = IoTrace.class.getDeclaredField("NULL_OBJECT");
            nullField.setAccessible(true);
            final Object nullObject = nullField.get(IoTrace.class);

            delegateField.set(IoTrace.class, nullObject);
        } else if(oldValue instanceof IoTraceMultiplexer) {
            final IoTraceMultiplexer multi = (IoTraceMultiplexer) oldValue;
            multi.removeDelegate(delegate);
        } else {
            throw new IllegalArgumentException("unable to remove, that IoTraceDelegate was not installed: " + delegate);
        }
    }
}
