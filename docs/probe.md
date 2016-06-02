---
title: title
---

Gumshoe Probe
=============

The gumshoe probe is the set of classes that receive, filter and forward information from the gumshoe hooks
or the JDK itself.  Unlike the hooks, the probe needs only to be in the classpath (not the bootclasspath).  

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
- Invoke JMX operations

Handling Data
-------------

Collected data is forwarded to a Listener at configured intervals.  
The probe package includes listeners to write data to standard out or a text file.

Configuration Properties
------------------------

System.properties can be used with Probe.initialize() or Probe.main(), 
or any Properties object can be used with Probe.initialize(Properties).
The values will install JVM hooks, manage filters and data collection,
and determine when and where results are reported.

For details:
- [Properties for socket I/O reporting](probe/properties-socket-io.md)
- [Properties for CPU usage reporting](probe/properties-cpu-stats.md)
- [Properties for unclosed socket reporting](probe/properties-unclosed-socket.md)

Managing Configuration with JMX
-------------------------------

Many of the settings can also to be managed at runtime using MBeans.
MBeans will be installed as part of Probe.initialize()
for any monitors enabled; or Properties can override this behavior
and install an MBean even if the monitor is not enabled during startup.
(This would allow you to later connect, enable the monitor,
and collect data only during a specific period of time.)

To work with MBeans, a JMX service must be enabled in the JVM.
Use system properties such as these:
  -Dcom.sun.management.jmxremote.port=1234
  -Dcom.sun.management.jmxremote.local.only=false
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false
and connect to your JVM using a JMX client such as jconsole.

By default, the MBeans installed will have names beginning with "com.dell.gumshoe".  
  
For details:
- [Socket I/O MBean](probe/jmx-socket-io.md)
- [CPU Stats MBean](probe/jmx-cpu-stats.md)
- [Unclosed Socket MBean](probe/jmx-unclosed-socket.md) 
