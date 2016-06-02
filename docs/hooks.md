---
title: title
---

Gumshoe Hooks
=============

Hooks are techniques used by gumshoe to collect information from the JVM that is filtered, accumulated and
finally reported by probes.  Some probes may share a hook, and some probes may work without a hook.

List of hooks:

[IoTrace](hooks/io-trace.md) is packaged in its own jar and must be loaded into the
bootclasspath of the JVM to support socket I/O and file I/O probes.

[SelectorProviderImpl](hooks/selector-provider.md) requires a System property
during startup to override the default factory to support NIO monitoring in
the socket I/O and datagram I/O probes.

[SocketFactoryImpl](hooks/socket-factory.md) is installed by the unclosed socket probe.

[DatagramFactoryImpl](hooks/datagram-socket-factory.md) is installed by the datagram I/O probe.

