Unclosed Socket Hook
====================

Summary
-------

The SocketFactoryImpl mechanism was used to define wrapper classes for regular (non-NIO) 
sockets that track closure and report sockets left open.  I/O operations are passed to
the original default factory generated sockets.