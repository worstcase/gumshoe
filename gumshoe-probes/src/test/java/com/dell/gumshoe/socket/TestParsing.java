package com.dell.gumshoe.socket;

import com.dell.gumshoe.socket.io.SocketEvent;
import com.dell.gumshoe.socket.io.SocketIODetailAdder;
import com.dell.gumshoe.socket.io.SocketReadEvent;
import com.dell.gumshoe.socket.io.SocketWriteEvent;
import com.dell.gumshoe.stack.Stack;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.text.ParseException;

import junit.framework.TestCase;

public class TestParsing extends TestCase {
    public void testDetailParse() throws Exception {
        SocketEvent event1 = SocketReadEvent.begin();
        SocketEvent event2 = SocketWriteEvent.begin();
        Thread.sleep(100);
        event1.complete(InetAddress.getLocalHost(), 5678, 123123);
        Thread.sleep(100);
        event2.complete(InetAddress.getLocalHost(), 5678, 321321);
        SocketIODetailAdder orig = new SocketIODetailAdder();
        orig.add(event1);
        orig.add(event2);

        String stringValue = orig.toString();

        SocketIODetailAdder copy = SocketIODetailAdder.fromString(stringValue);
        assertEquals(copy.targets, orig.targets);
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
