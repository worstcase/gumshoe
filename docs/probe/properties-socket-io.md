Properties for Socket I/O Monitoring
====================================

These properties are used by the Probe to install the hook, filter and forward socket I/O information generated.

Configuration Properties
------------------------

Initialization can use system properties by calling Probe.initialize() or with an explicit Properties argument.

    gumshoe.socket-io.period     Data samples will be reported at regular intervals (in milliseconds)
    gumshoe.socket-io.onshutdown If true, data samples will be reported when the JVM exits
    gumshoe.socket-io.mbean      If true, enable JMX control of socket usage probe
    gumshoe.socket-io.mbean.name Override name of JMX control
                                 (default is based on fully qualified class name) 
    gumshoe.socket-io.enabled    By default, the usage probe is initialized if periodic or shutdown
                                 reporting is enabled.  Override this behavior to install the probe
                                 now but enable/disable the reporting at another time.

Collection may be limited to certain networks, systems or ports: 
                              
    gumshoe.socket-io.include   Socket endpoints to monitor 
    gumshoe.socket-io.exclude   Socket endpoints not to monitor
    
                                Both of these properties support comma-separated list of: 
                                    <ip-address> : <mask-len> / <port-or-star>  
                                for example:
                                    192.168.3.0/24:80,127.0.0.1/32:*
                                    
                                Statistics for a socket are collected if the socket endpoint:
                                - matches "include" or "include" is blank
                                - does not match "exclude" or "exclude" is blank
         
Stacks should generally be [filtered](../filters.md) reduce overhead and simplify later analysis:
                                
    gumshoe.socket-io.filter...    See common filter properties [here](filter-properties.md) 

Collected data samples are written to:

    gumshoe.socket-io.output=none   Do not write samples (ie, when your program is
                                    adding its own explicit Listener to receive samples)
    gumshoe.socket-io.output=stdout Write to System.out (the default)
    gumshoe.socket-io.output=file:/some/path    Write to a text file.
