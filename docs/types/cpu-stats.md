Processor Utilization Data
==========================

Data
----

Periodic thread dumps are collected along with CPU statistics per thread.
Values reported include a count of threads performing each call stack per thread state (RUNNABLE, BLOCKED, WAITING, etc), accumulated user and total CPU time, count and time spent in blocked and waiting states.

Hooks
-----

Information is provided by the JVM's ThreadMXBean polled at periodic intervals.

Limitations
-----------

- Thread counts are cumulative over multiple samples.  So a single thread alternating between
  RUNNABLE and WAITING state sampled 5 times may be reported as 2 RUNNABLE threads and 3 WAITING.
  
- Per-thread CPU times and blocked/waiting times and counts
  reflect the totals for the thread, not necessarily the current call stack.
  
  Suppose you have an ExecutorService that is used to run two types of operations.  
  One is CPU intensive but short, no blocking or waiting,
  The other has a long wait for some event, then returns.
    
  One thread may perform a mix of both operations, but a thread dump at a point in time
  will show just the one currently running.
  
  Total CPU time, number of wait events, etc. at the time of the thread dump would be a total
  accumulated over execution of several operations of both types, not values associated with just the
  specific stack trace from the one operation currently running.

- Short-running infrequent tasks may completely escape collection.  Generally this is not an issue,
  as the statistics are intended to find the most use call stacks.  
  Use a java profiler (with significantly more overhead) if every method call is required.
  
  Certain situations could make this more likely.  If you collect thread dumps every minute, and
  the target application has a resource-intensive task performed every 5min, the schedules could align
  to miss ever reporting the task (or could align to catch it at the same point each time and over-represent
  its load).  In this case, vary the collection rate used.
  
- Not all JDKs expose blocking and waiting counts and times, so these values may not always be
  available.  The ThreadMXBean reports this ability with "isThreadContentionMonitoringSupported".

- In large applications, this probe can result in high CPU and memory overhead.
  Performing thread dumps less frequently will reduce CPU usage.   
  If memory is an issue, reduce stack size using coarser filters resulting in fewer frames.
