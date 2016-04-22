package com.dell.gumshoe;

import static org.junit.Assert.assertEquals;

import com.dell.gumshoe.network.AddressMatcher;
import com.dell.gumshoe.network.MultiAddressMatcher;
import com.dell.gumshoe.network.SocketIOAccumulator;
import com.dell.gumshoe.network.SocketIODetailAdder;
import com.dell.gumshoe.network.SocketIOMonitor;
import com.dell.gumshoe.network.SubnetAddress;
import com.dell.gumshoe.stack.FilterSequence;
import com.dell.gumshoe.stack.Stack;
import com.dell.gumshoe.stack.StackFilter;
import com.dell.gumshoe.stack.StandardFilter;
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
        AddressMatcher[] accept = { new SubnetAddress("127.0.0.1/32:1234") };
        AddressMatcher[] reject = { new SubnetAddress("127.0.0.1/32:*") };
        final MultiAddressMatcher socketFilter = new MultiAddressMatcher(accept, reject);

        SocketIOMonitor ioMonitor = new SocketIOMonitor(socketFilter, false);
        StackFilter filter = StandardFilter.builder().withEndsOnly(1, 0).build();
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
