package com.dell.gumshoe;

import static org.junit.Assert.assertEquals;

import com.dell.gumshoe.socket.SocketMatcher;
import com.dell.gumshoe.socket.SocketMatcherSeries;
import com.dell.gumshoe.socket.SubnetAddress;
import com.dell.gumshoe.socket.io.SocketIODetailAdder;
import com.dell.gumshoe.socket.io.SocketIOAccumulator;
import com.dell.gumshoe.socket.io.SocketIOMonitor;
import com.dell.gumshoe.stack.Filter;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stats.ValueReporter;

import org.junit.Ignore;
import org.junit.Test;

import java.net.InetAddress;
import java.util.Map;

public class TestIOAccumulator {
    int COUNT = 15;

    /** NOTE: can only run this test with project jarfile added to JVM bootclasspath */
    @Ignore
    @Test
    public void testCollectsStatistics() throws Exception {

        SocketMatcher[] accept = { new SubnetAddress("127.0.0.1/32:1234") };
        SocketMatcher[] reject = { new SubnetAddress("127.0.0.1/32:*") };
        final SocketMatcherSeries socketFilter = new SocketMatcherSeries(accept, reject);

        SocketIOMonitor ioMonitor = new SocketIOMonitor(socketFilter);
        StackFilter filter = Filter.builder().withEndsOnly(1, 0).build();
        SocketIOAccumulator ioAccumulator = new SocketIOAccumulator(filter);

        ValueReporter reporter = new ValueReporter("socket-io", ioAccumulator);
        ioMonitor.addListener(ioAccumulator);
        ioMonitor.initializeProbe();

        InetAddress addr = InetAddress.getByName("www.yahoo.com");
        InetAddress self = InetAddress.getLocalHost();

        for(int j=0;j<3;j++) {
            for(int i=0;i<COUNT;i++) {
                // 15 read ops
                Object memento = ioMonitor.socketReadBegin();
                Thread.sleep((long) (Math.random()*100));
                ioMonitor.socketReadEnd(memento, addr, 2344, 0, (long)(Math.random()*1000));

                // 15 read ops -- should NOT be reported
                memento = ioMonitor.socketReadBegin();
                Thread.sleep((long) (Math.random()*100));
                ioMonitor.socketReadEnd(memento, self, 54323, 0, (long)(Math.random()*1000));

                // 15 write ops
                memento = ioMonitor.socketWriteBegin();
                Thread.sleep((long) (Math.random()*100));
                ioMonitor.socketWriteEnd(memento, self, 1234, (long)(Math.random()*1000));
            }
            Thread.sleep(1000);

            Map<Stack,SocketIODetailAdder> stats = ioAccumulator.getStats();
            assertEquals(3, stats.size());
            ioAccumulator.reset();
        }
    }

}
