
Gumshoe Load Investigator 
=========================

Overview
--------

This tool profiles I/O operations and presents statistics over time per calling stack as a flame graph or root tree.

Gumshoe was first created initially for internal use in the Dell Cloud Manager application but
source code has since been released for public use under [these terms](LICENSE.rst).  

Packages
--------

* gumshoe-hooks

    A very small set of classes that must be loaded as part of the JVM bootclasspath to capture raw I/O.

* gumshoe-probe

    A queue and filter system to queue, filter and summarize I/O events and pass results to listeners.

* gumshoe-tools

    A swing GUI to configure the probe and display and manipulate results. 

Features
--------

Capture and visualize live socket I/O statistics and identify what is causing it.
View flame graph or root graph representation.
Filter stack frames at capture and/or during visualization, modify on the fly. 

Documentation
-------------

* Short intro and demo on youtube: [latest](https://www.youtube.com/watch?v=GGJFZfwXJ44) or the original [boring version](https://www.youtube.com/watch?v=1M9GX4ENMeI).

* [Quick start guide](QUICK-START.md) walks through using with a sample application.

* [User guide](docs/index.md)
