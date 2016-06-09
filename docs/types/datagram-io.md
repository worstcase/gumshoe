Datagram I/O Data
=================

Data
----

For each unique call stack that performs sends or receives a datagram, 
gumshoe reports totals for read, write and combined:
number of calls, number of bytes, and elapsed time.  

Samples collected can be filtered by IP, mask and port to limit reports to only certain systems or services.

Hooks
-----

Gumshoe uses a [DatagramSocketImplFactory](../hooks/datagram-socket-factory.md)
to report regular (non-NIO) datagram I/O using an extension similar to IoTrace.

For NIO operations, a custom [SelectorProvider hook](../hooks/selector-provider.md) 
wraps default implementations to report I/O.

Datagram operations are reported as events which are queued and handled off the I/O thread.  
The [event handler](../probe/event-handling.md) accumulates these events 
and reports a total per calling stack.

Limitations
-----------

- NIO non-blocking operations are asynchronous, so the time reported is really the time to pass
  the data into a buffer, not the actual I/O time.  The value is real and usable, representing
  how long the target application waited trying to perform I/O using the NIO library, but
  is not the same as TCP transmission time included in the non-NIO values for these statistics

- The event queue between IoTrace and the statistics accumulator can back up if I/O events are
  happening faster than the accumulator can process them.  This can result in two issues:
  
  - Statistics totals may lag the times reported.  For example, a summary of I/O reported at 12:00
    may actually only include events up to 11:59.  
  - While the queue capacity is exceeded, events may be dropped and a message is shown in STDOUT

  If this is happening occasionally during peak loads, it may not be an issue.  I/O statistics are still
  gathered -- it just samples fewer operations when the queue is full.  If this is happening a lot during the loads
  being tested then look at then [event handler configuration](../probe/event-handling.md).
