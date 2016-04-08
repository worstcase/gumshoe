Gumshoe Probe
=============

The gumshoe probe is the set of classes that receive, filter and forward information from the gumshoe hooks.
Unlike the hooks, the probe needs only to be in the classpath (not the bootclasspath).  

Managing Data Collection
------------------------

The com.dell.gumshoe.Probe class is a utility class that manages components that listen and forward
gumshoe data and can be managed directly, using JMX or using Properties.

The Probe is used to initialize and begin monitoring:
- Use probe as the main class on startup and have it invoke your application's main
- Add a servlet context listener or other lifecycle callback mechanism to call Probe from within your application
- Explicitly invoke Probe methods from your application

It can stop monitoring or update configuration:
- Explicitly invoke Probe methods from your application
- _soon_ Invoke JMX operations

Handling Data
-------------

Collected data is forwarded to a  
[Listener](https://github.com/dcm-oss/gumshoe/blob/master/gumshoe-probes/src/main/java/com/dell/gumshoe/socket/SocketIOStackReporter.java#L50) 
at configured intervals.  The probe package includes listeners to write data to standard out or a text file.

Configuration Properties
------------------------

Initialization can use system properties by calling Probe.initialize() or with an explicit Properties argument.

    gumshoe.socket-io.period    Data samples will be reported at regular intervals (in milliseconds)
    gumshoe.socket-io.onshutdown    If true, data samples will be reported when the JVM exits
    gumshoe.socket-io.mbean     If true, enable JMX control of socket usage probe (not implemented)
    gumshoe.socket-io.enabled   By default, the usage probe is initialized if periodic or shutdown
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
         
Stacks should generally be [filtered](filters.md) reduce overhead and simplify later analysis:
                                
    gumshoe.socket-io.filter.exclude-jdk    Exclude frames from java built-in packages and gumshoe 
    gumshoe.socket-io.filter.include        Include only these packages or classes (comma-separated)
    gumshoe.socket-io.filter.exclude        Exclude these packages or classes 
    gumshoe.socket-io.filter.top            Number of frames at top of stack to retain
    gumshoe.socket-io.filter.bottom         Number of frames at bottom of stack to retain
    gumshoe.socket-io.filter.allow-empty-stack    If filters excluded all frames from a stack,
                                                  the full unfiltered stack can be restored (if false),
                                                  or the empty stack will be used and collect stats
                                                  as an "other" category.
    gumshoe.socket-io.filter.none           If true, override other filter settings: no filtering is done. 

Collected data samples are written to:

    gumshoe.socket-io.output=none   Do not write samples (ie, when your program is
                                    adding its own explicit Listener to receive samples)
    gumshoe.socket-io.output=stdout Write to System.out (the default)
    gumshoe.socket-io.output=file:/some/path    Write to a text file.
