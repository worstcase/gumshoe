package com.dell.gumshoe.tools;

import com.dell.gumshoe.socket.SocketIOListener.DetailAccumulator;
import com.dell.gumshoe.stack.Stack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class TestFileReader extends TestCase {
    FileDataParser target;

    @Override
    public void setUp() throws FileNotFoundException {
        final String fileName = getClass().getClassLoader().getResource("sample-data.txt").getFile();
        target = new FileDataParser(new RandomAccessFile(fileName, "r"));
    }

    @Override
    public void tearDown() throws IOException {
        target.close();
    }

    public void testNavigating() throws Exception {
        Map<Stack, DetailAccumulator> entry = target.getNextSample();
        assertEquals(7, entry.size());
        Date time1 = target.getSampleTime();

        entry = target.getNextSample();
        Date time2 = target.getSampleTime();
        assertEquals(2, entry.size());
        assertTrue(time1.before(time2));

        entry = target.getNextSample();
        Date time3 = target.getSampleTime();
        assertEquals(2, entry.size());
        assertTrue(time2.before(time3));

        entry = target.getPreviousSample();
        Date time4 = target.getSampleTime();
        assertEquals(2, entry.size());
        assertEquals(time2, time4);

        entry = target.getPreviousSample();
        assertNotNull(entry);
        assertNotNull(target.getSampleTime());
        entry = target.getPreviousSample();
        assertNull(entry);
        assertNull(target.getSampleTime());
    }

    public void testDump() throws Exception {
        target.parseFile();
        List<Date> times = new ArrayList<>(target.getSampleTimes());
        assertEquals(3, times.size());

        Date middle = times.get(1);
        Map<Stack, DetailAccumulator> entry = target.getSample(middle);
        Date time = target.getSampleTime();
        assertEquals(2, entry.size());
        assertEquals(time, middle);

        entry = target.getSample(new Date());
        assertNull(entry);
        assertNull(target.getSampleTime());
    }
}
