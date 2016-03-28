
Gumshoe Load Investigator JVM Hooks 
===================================

Overview
--------

Gumshoe adds a hook in the JVM to monitor socket and file I/O.  The monitored JVM must be run
using the commandline:

    java -bootclasspath/p gumshoe-hooks.jar ... 


More Detail
-----------

Gumshoe Load Investigator measures socket and file I/O using the sun.misc.IoTrace class.
The built-in implementation has several empty methods that are called before and after each I/O
operation.   This package overrides this implementation with one that allows gumshoe to handle
these calls and collect statistics.  

Because it has to override a built-in class from rt.jar, the contents of this module are included 
in the bootclasspath before rt.jar.  Then any application that wants to receive the IoTrace callbacks
can implement an interface IoTraceDelegate and install it with IoTraceUtil.addTrace().


Performance Note
----------------

The IoTrace callbacks execute before and after every read or write operation on every socket or file.
It can affect performance.  Specifically, it will add CPU and memory overhead to track the I/O.
For applications that are constrained by I/O performance, this is not usually a problem. 
Regardless, this overhead can be removed by omitting the -bootclasspath/p option. The original 
IoTrace class from rt.jar is used, resulting in no additional system load per I/O operation.
