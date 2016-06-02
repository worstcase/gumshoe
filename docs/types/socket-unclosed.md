Unclosed Socket Monitor
=======================

Data
----

All open sockets are tracked with the original calls stack that opened them.  Number of sockets per unique call
stack is reported along with the max length a socket has been open.

Hooks
-----

A [custom SocketFactoryImpl](../hooks/socket-factory.md) wraps each regular (non-NIO) socket created 
to track the original call stack and creation time.

Limitations
-----------

NIO sockets are not currently tracked.