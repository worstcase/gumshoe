Stack Filters
=============

Using the right stack filters is probably the most important part of getting good results from gumshoe.
Without any filters, gumshoe will probably generate a flame graph showing that Thread.run() and YourClass.main() 
are the points of entry leading to all of your network I/O; and a root graph showing SocketInputStream.read() and
SocketOutputStream.write() are the calls directly making the I/O requests.  Instead you will filter the stack
frames to identify exactly what is relevant to your application and the current network load you are investigating.

As a side note, there are really two places where stack frames are filtered.  In the probe, a loose filter
can drop the most obvious candidates to reduce the memory usage and file size while collecting data.
Then in the viewer a more restrictive filter can better refine the view.  To start, maybe just exclude
the JDK and gumshoe classes in the probe (the default).  After collecting and looking at reports from
your application, you will better be able to identify other packages and classes to exclude from collection.

I'll describe ONE APPROACH I've used to get good results from gumshoe.  I'm sure this isn't the only way
and may well not be the best, so I'd love to hear about your other ways of approaching the problem.
But this has worked for me, so I'll describe it.

I tend to think of my call stack as if it just had a few sections.  In the sequence they occur (but opposite
the order they appear in the stack trace) they are:
    "container" -- JDK, app server or other overhead somehow getting the request into your application
    "point of entry" -- your code first gets a request to do something
    "the middle part" -- the steps needed to carry out the action requested
    "point of exit" -- a step in the operation that has to update some outside resource (file, DB, another app)
    "implementation" -- drivers, protocols, etc to carry out the outside request 

Depending on the issue at hand, where these sections split even in the same application may be different.
For example, you might want to compare REST vs SOAP stats,
so the point of entry would be somewhere in the REST or SOAP libraries,
and we could filter out JDK and HTTP packages below that to see 
REST and SOAP side by side on the bottom of a flame graph.

You might then want to compare individual user operations in your app.  Suppose it's an identity management app
you want to compare stats for the main services CreateUser, UpdateUser, DeleteUser, AuthenticateUser.
Now  filter out the same REST and SOAP libs you were just looking at and the flame graph should leave
these four services across the bottom.  Stats shown for each include totals from both REST and SOAP calls to them.

Different filters and views of the same data help you answer different questions.

Matching Classes
----------------

Included and excluded names are defined by the gumshoe.socket-io.filter.include and gumshoe.socket-io.filter.exclude
properties.  The values are matched by checking if the fully qualified class name of the stack frame 
[startsWith](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#startsWith(java.lang.String))
the pattern.  So for example, if you exclude "com.de" you would exclude "com.dell" and "com.derby" frames
from the resulting stack.  

The option to exclude JDK and gumshoe classes specifically excludes packages: 
com.dell.gumshoe., java., javax., sun. and sunw., 
each ending with a dot, so for example, sunny.foo.Bar is not excluded.

Top and Bottom
--------------

You can limit the number of frames at the top and bottom of the stack as well.
This is sometimes useful after you have a good set of class filters 
that carefully identify the points of entry and exit you are interested in.
It will remove the middle business logic and leave just those points of entry and exit
or a couple frames showing the main branches at the top or bottom.

Simplification
--------------

Sometimes the full frame is not needed and can be simplified to drop line number, inner classes, etc.
There are four levels of simplification that can be performed:
    NO_LINE_NUMBERS     drop line numbers
    NO_METHOD           drop line number and method name
    NO_INNER_CLASSES    drop line number, method and inner classes
    NO_CLASSES          keep only the package name

Empty Stacks
------------

If a noticeable portion of I/O is appearing as either an empty stack or an unfiltered stack,
your filter is probably too broad.  For example, some frameworks maintain threadpools that handle requests
asynchronously and the full call stack performing the I/O may not involve any calls from your
application packages.

Custom Filters
--------------

You can also implement [StackFilter](https://github.com/dcm-oss/gumshoe/blob/master/gumshoe-probes/src/main/java/com/dell/gumshoe/stack/StackFilter.java) or [FrameMatcher](https://github.com/dcm-oss/gumshoe/blob/master/gumshoe-probes/src/main/java/com/dell/gumshoe/stack/FrameMatcher.java) directly to help visualize specific situations or 
combine multiple conditions. 

Socket Address
--------------

Not a StackFilter but a related idea, statistics can also be collected only for specific network and port addresses.
With stacks, all load statistics are retained, the filtering just affects how they are grouped and reported.
With a socket matcher, only  matching statistics are retained.  For example, traffic to an external logging system
might be dropped completely. 
