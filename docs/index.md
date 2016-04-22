Gumshoe User Guide
==================

Overview
--------

Gumshoe intercepts java calls to sockets and captures network statistics.
Data can be logged to a file or viewed live.

[Features](features.md)
-----------------------

- Network I/O analysis (TCP and UDP)
- CPU utilization analysis
- unclosed socket detection  
- file I/O analysis

Getting Started
---------------

- Examples
- ELI5: Performance tuning
- Components of gumshoe
- Step by step


Collecting Samples
------------------

- [About the hook](hook.md)
- [Configuring the probe](probe.md)
- [Using filters](filters.md)
- [Running your program](run.md)

Viewing Samples
---------------

- Running gumshoe GUI from your JVM
- Running standalone GUI
- Selecting a data sample
- Navigating the graph
- Graph display options
- Configuring filters

Understanding Performance
-------------------------

- Identifying bottlenecks
- Stacks vs Statistics
- Scalability