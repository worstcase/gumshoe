Properties for File I/O Monitoring
==================================

These properties are used by the Probe to install the hook, filter and forward file I/O statistics generated.

Configuration Properties
------------------------

Initialization can use system properties by calling Probe.initialize() or with an explicit Properties argument.

    gumshoe.file-io.period     Data samples will be reported at regular intervals (in milliseconds)
    gumshoe.file-io.onshutdown If true, data samples will be reported when the JVM exits
    gumshoe.file-io.mbean      If true, enable JMX control of file usage probe
    gumshoe.file-io.mbean.name Override name of JMX control
                               (default is based on fully qualified class name) 
    gumshoe.file-io.enabled    By default, the usage probe is initialized if periodic or shutdown
                               reporting is enabled.  Override this behavior to install the probe
                               now but enable/disable the reporting at another time.

Collection may be limited to certain file or directories: 
                              
    gumshoe.file-io.include   Paths to monitor 
    gumshoe.file-io.exclude   Paths not to monitor
    
                                Both of these properties support comma-separated list of wildcard expressions.
                                For example:
                                    /tmp/**, *.dat, **/bob/**
                                    
                                Statistics are collected if the file path:
                                - does not matches "exclude" or "exclude" is blank
                                - matches "include"
                                
Stacks should generally be [filtered](filters.md) reduce overhead and simplify later analysis:
                                
    gumshoe.file-io.filter...    See common filter properties [here](filter-properties.md) 


Collected data samples are written to:

    gumshoe.file-io.output=none   Do not write samples (ie, when your program is
                                    adding its own explicit Listener to receive samples)
    gumshoe.file-io.output=stdout Write to System.out (the default)
    gumshoe.file-io.output=file:/some/path    Write to a text file.
