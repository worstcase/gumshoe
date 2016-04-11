package com.dell.gumshoe.socket;

import com.dell.gumshoe.socket.io.IODetail;
import com.dell.gumshoe.socket.io.SocketIODetailAdder;
import com.dell.gumshoe.stack.Stack;

import java.text.MessageFormat;
import java.text.ParseException;

import junit.framework.TestCase;

public class TestParsing extends TestCase {
    public void testDetailParse() throws ParseException {
        SocketIODetailAdder orig = new SocketIODetailAdder();
        orig.add(new IODetail("1.2.3.4/5678", 123123, 123, 321321, 321));

        String stringValue = orig.get().toString();

        SocketIODetailAdder copy = SocketIODetailAdder.fromString(stringValue);
        assertEquals(copy.addresses, orig.addresses);
        assertEquals(copy.readBytes.get(), orig.readBytes.get());
        assertEquals(copy.readCount.get(), orig.readCount.get());
        assertEquals(copy.readTime.get(), orig.readTime.get());
        assertEquals(1, orig.readCount.get());
        assertEquals(copy.writeBytes.get(), orig.writeBytes.get());
        assertEquals(copy.writeCount.get(), orig.writeCount.get());
        assertEquals(copy.writeTime.get(), orig.writeTime.get());
        assertEquals(1, orig.writeCount.get());
    }

    public void testStackParse() {
        StackTraceElement orig = new StackTraceElement("foo", "bar", "baz", 123);
        String stringValue = Stack.FRAME_PREFIX + orig.toString();
        StackTraceElement copy = Stack.parseFrame(stringValue);
        assertEquals(orig, copy);

        StackTraceElement sample = Stack.parseFrame("    at com.enstratus.task.TaskBlocker.getTaskInfo(TaskBlocker.java:40)");
        assertEquals("com.enstratus.task.TaskBlocker", sample.getClassName());
        assertEquals("getTaskInfo", sample.getMethodName());
        assertEquals(40, sample.getLineNumber());
    }

    public void testTagParse() throws ParseException {
        String line = "<gumshoe-report type='socket-io' time='2016-01-05 22:31:05'>";
        Object[] parts = new MessageFormat("<gumshoe-report type=''socket-io'' time=''{0}''>").parse(line);
        String timePart = (String) parts[0];
        assertEquals("2016-01-05 22:31:05", timePart);
    }
}
