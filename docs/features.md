Gumshoe Features
================

Data Collection
---------------

These kinds of resource usage are monitored and reported:
- [socket I/O (TCP)](types/socket-io.md)
- [file I/O](types/file-io.md)
- [CPU utilization](types/cpu-stats.md)
- [datagram I/O (UDP)](types/datagram-io.md)
- [unclosed socket detection](types/socket-unclosed.md)

Visualization
-------------

All data collection is associated with a specific call stack, so information is not "system-wide"
but tied to individual threads and method calls.  If multiple threads include the same method call,
resource usage can be combined across threads for a total for that method call over all similar threads.
This can be presented by combining identical frames starting at the bottom of the call stack, 
which results in a flame graph; or starting at the bottom, which results in a root graph.

Live capture and view 
---------------------

Often data is collected from the target application in a text file and analyzed later with the gumshoe viewer.
However, the viewer can also be launched from within the same JVM and data viewed as it is collected.