Running your Program
====================

Direct Invocation
-----------------

Suppose your application starts with a command such as:

    java -classpath dist/JavaChat.jar:lib/* javachat.JavaChat --server --port 1234
    
or more generally:

    java -classpath <your-classes> <other-jdk-options> <main-class> <program-arguments>
    
Then you'll want to make these changes to the command:

- add -javaagent to install our hooks
- add -Dproperties to configure the probes

For example, to use default filters and report every 5min to STDOUT:

    java -javaagent:$LIB/gumshoe-agent.jar \
         -classpath dist/JavaChat.jar:lib/* \
         -Dgumshoe.socket-io.period=300000 \
         javachat.JavaChat --server --port 1234
         
Custom Integration         
------------------

With container or some project designs it isn't practical to modify the main class,
so a lifecycle listener, JMX trigger or some other mechanism might be needed in your code
to start the probe.

Add a snippet like this: 

    ProbeManager mgr = new ProbeManager();
    mgr.initialize();  // use System.properties()
    
Or to avoid System.properties():

    Properties p = new Properties();
    p.load( someReader );
    ProbeManager mgr = new ProbeManager();
    mgr.initialize( p );
