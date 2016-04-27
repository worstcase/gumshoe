Datagram I/O Hook
=================

Summary
-------

The DatagramSocketFactoryImpl mechanism was used to define wrapper classes for regular (non-NIO) 
datagram sockets that report I/O statistics to IoTraceAdapter.  The wrappers invoke the actual datagram operations
on the original default factory implementation while tracking operations, size and elapsed time.
