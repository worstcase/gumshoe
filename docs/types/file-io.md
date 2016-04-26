File I/O Data
=============

Data
----

For each unique call stack that reads or writes file data, 
gumshoe reports totals for read, write and combined:
number of calls, number of bytes, and elapsed time.  

Samples collected can be filtered with wildcards to limit results to certain file names, directories or filesystems.

Hooks
-----

File I/O collection relies on a callback mechanism defined in [IoTrace](../hooks/io-trace.md).
To use this mechanism the gumshoe hook must override the JRE version of IoTrace
using the [bootclasspath argument](../hook.md).

Operations are reported as events which are queued and handled off the I/O thread.  
The [event handler](../probe/event-handling.md) accumulates these events 
and reports a total per calling stack.

Limitations
-----------

- NIO non-blocking operations are asynchronous, so the time reported is really the time to pass
  the data into a buffer, not the actual read or write time.  The value is real and usable, representing
  how long the target application waited trying to perform I/O using the NIO library, but
  is not the same as hardware read/write time shown in the non-NIO values for these statistics

- The event queue between IoTrace and the statistics accumulator can back up if I/O events are
  happening faster than the accumulator can process them.  This can result in two issues:
  
  - Statistics totals may lag the times reported.  For example, a summary of I/O reported at 12:00
    may actually only include events up to 11:59.  
  - While the queue capacity is exceeded, events may be dropped and a message is shown in STDOUT

  If this is happening occasionally during peak loads, it may not be an issue.  I/O statistics are still
  gathered -- it just samples fewer when the queue is full.  If this is happening a lot during the loads
  being tested then it can be addressed.  
  
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
    [processor utilization statistics](processor.md) before socket I/O.
    
    Or it could be due to gumshoe stack filters.  Each stack filter configured has to
    modify the event call stack on the same event handling thread.  Complex filters
    (such as the recursion filter) or deep call stacks can result in more load than the
    thread can handle.  Relax [filters](../filters.md) (at the expense of more memory use) or increase the
    event handler thread priority.      
    
  If the event queue is full:
  
  - Ignore it (_really!, it isn't so bad..._)
  
    If the problem is intermittent, it may not affect all samples, 
    and data reported in those affected is still likely a representative subset of all I/O.
    Total I/O values will not be accurate but the relative I/O comparisons between threads
    should still provide insight into what the target application is doing to generate the I/O load.
    
  - Increase queue size
  
    If the problem is intermittent, then a larger queue can let the handler thread
    catch up after load spikes.  However, if load is consistently over the handler capacity,
    this will just delay and not fix the problem.  (Requires restart)
    
  - Increase handler thread priority
  
    Socket and file I/O events perform all filtering and accumulation functions on the
    handler thread.  The default is to run at Thread.MIN_PRIORITY, reflecting the decision to
    risk dropping data rather than impact the target application.  This can be changed to a
    higher value to reduce dropping events even if it means taking some CPU time away from
    the target application.
