package com.dell.gumshoe.tools;

import com.dell.gumshoe.inspector.FileDataParser;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stats.StatisticAdder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class TestFileReader extends TestCase {
    FileDataParser target;

    @Override
    public void setUp() throws Exception {
        final String fileName = getClass().getClassLoader().getResource("sample-data.txt").getFile();
        target = new FileDataParser(fileName);
    }

    @Override
    public void tearDown() throws IOException {
        target.close();
    }

    public void testNavigating() throws Exception {
        Map<Stack, StatisticAdder> entry = target.getNextReport();
        assertEquals(7, entry.size());
        Date time1 = target.getReportTime();

        entry = target.getNextReport();
        Date time2 = target.getReportTime();
        assertEquals(2, entry.size());
        assertTrue(time1.before(time2));

        entry = target.getNextReport();
        Date time3 = target.getReportTime();
        assertEquals(2, entry.size());
        assertTrue(time2.before(time3));

        entry = target.getPreviousReport();
        Date time4 = target.getReportTime();
        assertEquals(2, entry.size());
        assertEquals(time2, time4);

        entry = target.getPreviousReport();
        assertNotNull(entry);
        assertNotNull(target.getReportTime());
        entry = target.getPreviousReport();
        assertNull(entry);
        assertNull(target.getReportTime());
    }

    public void testDump() throws Exception {
        target.parseFile();
        List<Date> times = new ArrayList<>(target.getReportTimes());
        assertEquals(3, times.size());

        Date middle = times.get(1);
        Map<Stack, StatisticAdder> entry = target.getReport(middle);
        Date time = target.getReportTime();
        assertEquals(2, entry.size());
        assertEquals(time, middle);

        entry = target.getReport(new Date());
        assertNull(entry);
        assertNull(target.getReportTime());
    }
}
