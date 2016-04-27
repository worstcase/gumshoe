IoTrace Hook
============

Summary
-------

To collect socket or file I/O statistics, the target JVM must include the option:

    -Xbootclasspath/p <gumshoe-hooks.jar>


Details
-------

At its core, gumshoe collects socket and file I/O data using a callback 
mechanism built into the JRE.
Socket and file operations make a call to an empty class IoTrace before and after
each read or write operation.  The gumshoe hook replaces this empty class with one
that can report the details of those operations and the call stack where it occurred.

To override a built-in class from the JRE rt.jar file, the target application
will need to start with a option shown above.    
Failure to include this option will not prevent your application from running,
but gumshoe will be unable to collect data about socket or file I/O.
Other kinds of statistics can still be collected.

Having the hook in place will allow applications to start and stop monitoring
as needed, and it will introduce almost no overhead when not monitoring.
Specifically, instead of the default empty method in IoTrace, the replacement
[IoTrace](../../gumshoe-hooks/src/main/java/sun/misc/IoTrace.java) 
methods make one call into a 
[null object](https://en.wikipedia.org/wiki/Null_Object_pattern) IoTraceAdapter

The IoTraceAdapter used provides additional methods to collect datagram I/O
using similar methods.  The datagram hooks report directly to this adapter
and not sun.misc.IoTrace, however, so the original class signature of
IoTrace is unchanged.