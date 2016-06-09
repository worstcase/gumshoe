File I/O Data
=============

Data
----

For each unique call stack that reads or writes file data, 
gumshoe reports totals for read, write and combined:
number of calls, number of bytes, and elapsed time.  

Samples collected can be filtered with wildcards to limit reports to certain file names, directories or filesystems.

Hooks
-----

File I/O collection relies on a callback mechanism defined in [IoTrace](../hooks/io-trace.md).
To use this mechanism the gumshoe hook must override the JRE version of IoTrace
using the bootclasspath argument.

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
  gathered -- report just contain samples fewer when the queue is full.  If this is happening a lot during the loads
  being tested then look at then [event handler configuration](../probe/event-handling.md).
  
