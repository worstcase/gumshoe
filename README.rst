
Gumshoe Load Investigator 
=========================

Overview
--------

This tool profiles I/O operations and presents statistics over time per calling stack as a flame graph or root tree.

The tool was first created initially for internal use at Dell and source code has been released
for public use under :doc:`these terms<LICENSE.rst>`.  

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

* :doc:`Quick start guide<QUICK-START.rst>` walks through using with a sample application.

