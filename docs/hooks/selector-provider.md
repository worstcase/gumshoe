NIO Monitoring Hook
===================

The NIO SelectorProviderImpl mechanism is used to define wrapper classes that add missing IoTrace callsback
to NIO socket non-blocking reads and IoTraceAdapter callsbacks to datagram NIO operations.  IO is passed
to the original default implementation and the wrapper only provides event reporting.

Without the hook, all NIO socket writes, and blocking NIO socket reads and all file I/O are still reported.
The hook is needed for non-blocking socket reads and datagram I/O only.  

To use the hook, the JVM must start with system property:

    java.nio.channels.spi.SelectorProvider=com.dell.gumshoe.network.IoTraceSelectorProvider