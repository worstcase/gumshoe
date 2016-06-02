I/O Probe Event Handler
=======================

The socket, datagram and file I/O probes each create entries in a (separate) bounded queue 
for each read and write operation to offload statistics tabulation from the thread 
performing I/O.  If the consumer of the thread is not able to keep up with the events
being generated, the queue will reach capacity and events may be lost.

Monitoring the Queue
--------------------  

If an event queue fills, a message is displayed on STDOUT such as:

    GUMSHOE: IO event queue for SocketIOMonitor is full

If this message is not found in STDOUT, then no further investigation or configuration is needed.
If it is seen, use JMX MBeans to determine how many events are being lost.

SocketIOProbe, DatagramIOProbe and FileIOProbe each have corresponding MBeans with attributes and operations:

    QueueStatisticsEnabled  set true to collect number of events dropped and average queue size
    QueueStats              text description of average and max queue size, number of events and % dropped
    resetQueueStats()       operation to reset counters to zero
    
Also to enable collecting queue statistics during startup, System properties can be used:

    gumshoe.socket-io.handler.stats-enabled=true    
    gumshoe.datagram-io.handler.stats-enabled=true    
    gumshoe.file-io.handler.stats-enabled=true    
    
Using these controls, enable statistics collection and monitor the target application for some time.
Then look at QueueStats to see what portion of events are dropped.

Live with It?
-------------

In most cases, dropping even a significant fraction of the events is not a problem.  The events 
received should still be a representative sample of all I/O and although the total counts and number of bytes
may not be accurate, the relative weight of each stack and frame should still be usable.

Deal with It!
-------------

If you decide you are dropping more events than you are comfortable with, there are a number of ways
to improve reporting.

  - if there are only intermittent periods of dropped events, increasing queue size may be enough to
    let the consumer handle events in between peaks.  Use properties to set the queue size (default is 500):
    
    gumshoe.socket-io.handler.queue-size=1000    
    gumshoe.datagram-io.handler.queue-size=1000    
    gumshoe.file-io.handler.queue-size=1000    
     
  The queue will fill if events are produced (individual I/O operations) faster than they are consumed.
  Some possible reasons:
  
  - The target application is performing a lot of small network operations
  
    This could be an area to improve the target application.  
    Lots of small operations are less efficient than fewer large operations.
       
    Or this could just be the nature of the expected application load,
    so increase the gumshoe event queue size and the handler thread priority to accommodate. 
        
  - The JVM is CPU bound
  
    The event queue may back up if the target application is CPU bound.  This could be
    an issue in the target application itself, and you may want to look at
    [processor utilization statistics](../types/cpu-stats.md) before socket I/O.
    
    Or it could be due to gumshoe stack filters.  Each stack filter configured has to
    modify the event call stack on the same event handling thread.  Complex filters
    (such as the recursion filter) or deep call stacks can result in more load than the
    thread can handle.  Relax [filters](../filters.md) (at the expense of more memory use) 
    or increase the number of threads or the event handler thread priority.
