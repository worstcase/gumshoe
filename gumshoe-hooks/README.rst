
Gumshoe Load Investigator JVM Hooks 
===================================

Overview
--------

Gumshoe adds a hook in the JVM to monitor socket and file I/O.  The monitored JVM must be run
using the commandline:

    java -Xjavaagent:/PATH/TO/gumshoe-agent.jar ... 


More Detail
-----------

Gumshoe Load Investigator measures socket and file I/O using the sun.misc.IoTrace class.
The built-in implementation has several empty methods that are called before and after each I/O
operation.   This package replaces this implementation with one that allows gumshoe to handle
these calls and collect statistics.  Then any application that wants to receive the IoTrace callbacks
can implement an interface IoTraceListener and install it with IoTraceHandler.addTrace().


Performance Note
----------------

The IoTrace callbacks occur before and after every read or write operation on every socket or file.
Without using gumshoe at all, there is CPU overhead from two empty method calls per I/O operation.
With them gumshoe agent installed but not collecting socket or file I/O, this increases slightly --
the empty method is now replaced by a method with one line that calls an empty delegate method.
So even in CPU-constrained applications, the agent alone should not add much overhead.

When monitoring is enabled, there is additional CPU and memory overhead involved in collecting
and accumulating I/O statistics.  In many cases this is not an issue; when there is a
network or file I/O bottleneck, additional CPU and memory can be used without affecting
overall system performance.  

If CPU or memory are an issue, overhead can be reduced in several ways:

- Divide and conquer: use fewer probes

  Collect file I/O in one pass, than and network I/O at a different time (instead of collecting both at once).
  
- Divide and conquer: limit targets collected

  Select by IP address or directory to collect less information at a time. For example, collect network data 
  from your LAN in one pass, then collect samples from outside your LAN in another pass.

- Reduce clutter: use stack filters

  Stack filters can reduce the size of the stacks collected significantly and make it much easier to
  spot the parts of code resulting in the I/O measured.  Stack filters cause some additional CPU overhead
  when each sample is collected, but can reduce memory significantly.
  