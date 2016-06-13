Properties for Datagram (UDP) I/O Monitoring
====================================

These properties are used by the ProbeManager to install the hook, filter and forward datagram I/O information generated.

Configuration Properties
------------------------

Initialization can use system properties by calling ProbeManager.initialize() or with an explicit Properties argument.

    gumshoe.datagram-io.period     Data reports will be generated at regular intervals (in milliseconds)
    gumshoe.datagram-io.onshutdown If true, a report will be generated when the JVM exits
    gumshoe.datagram-io.mbean      If true, enable JMX control of datagram usage probe
    gumshoe.datagram-io.mbean.name Override name of JMX control
                                 (default is based on fully qualified class name) 
    gumshoe.datagram-io.enabled    By default, the usage probe is initialized if periodic or shutdown
                                 reporting is enabled.  Override this behavior to install the probe
                                 now but enable/disable the reporting at another time.

Collection may be limited to certain networks, systems or ports: 
                              
    gumshoe.datagram-io.include   datagram endpoints to monitor 
    gumshoe.datagram-io.exclude   datagram endpoints not to monitor
    
                                Both of these properties support comma-separated list of: 
                                    <ip-address> : <mask-len> / <port-or-star>  
                                for example:
                                    192.168.3.0/24:80,127.0.0.1/32:*
                                    
                                Statistics for a datagram are collected if the datagram endpoint:
                                - matches "include" or "include" is blank
                                - does not match "exclude" or "exclude" is blank
         
Stacks should generally be [filtered](../filters.md) reduce overhead and simplify later analysis:
                                
    gumshoe.datagram-io.filter...    See common filter properties [here](filter-properties.md) 

Collected data reports are written to:

    gumshoe.datagram-io.output=none   Do not write reports (ie, when your program is
                                    adding its own explicit Listener to receive reports)
    gumshoe.datagram-io.output=stdout Write to System.out (the default)
    gumshoe.datagram-io.output=file:/some/path    Write to a text file.
    
The event queue and consumer can be monitored and managed using:

    gumshoe.datagram-io.handler.stats-enabled   Should statistics be collected on event queue size
                                                and number of events dropped due to a full 
                                                queue (default is false)    
    gumshoe.datagram-io.handler.queue-size      Number of events that can be queued (default is 500)
    gumshoe.datagram-io.handler.count           Number of consumer threads (default 1)
    gumshoe.datagram-io.handler.priority        Thread priority (default 1=Thread.MIN_PRIORITY)

See [event queue documentation](event-handling.md) for more information.
