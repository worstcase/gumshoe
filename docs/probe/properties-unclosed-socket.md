Properties for Unclosed Socket Reporting
========================================

These properties are used by the Probe
to install the hook, filter stacks, and forward information generated about unclosed sockets.

Configuration Properties
------------------------

Initialization can use system properties by calling Probe.initialize() or with an explicit Properties argument.

    gumshoe.socket-unclosed.period     Data samples will be reported at regular intervals (in milliseconds)
    gumshoe.socket-unclosed.onshutdown If true, data samples will be reported when the JVM exits
    gumshoe.socket-unclosed.mbean      If true, enable JMX control of socket usage probe
    gumshoe.socket-unclosed.mbean.name Override name of JMX control
                                       (default is based on fully qualified class name) 
    gumshoe.socket-unclosed.enabled    By default, the usage probe is initialized if periodic or shutdown
                                       reporting is enabled.  Override this behavior to install the probe
                                       now but enable/disable the reporting at another time.

Stacks should generally be [filtered](../filters.md) reduce overhead and simplify later analysis:
                                
    gumshoe.socket-unclosed.filter...    See common filter properties [here](filter-properties.md) 

Collected data samples are written to:

    gumshoe.socket-unclosed.output=none   Do not write samples (ie, when your program is
                                          adding its own explicit Listener to receive samples)
    gumshoe.socket-unclosed.output=stdout Write to System.out (the default)
    gumshoe.socket-unclosed.output=file:/some/path    Write to a text file.
