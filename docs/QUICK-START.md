---
title: title
---


Overview
--------

For this example, we'll use a simple freeware chat program to demonstrate the features of gumshoe.

Step 1: Get gumshoe
-------------------

1. Make sure your system has maven3, ant, JDK7, probably some other stuff.

2. Get gumshoe.

    Download and unpack gumshoe source.

        wget https://github.com/dcm-oss/gumshoe/archive/master.zip
        unzip gumshoe-master.zip
        cd gumshoe-master
        export GUMSHOE_HOME=`pwd` # maybe put this into .bashrc too?

3. Build

    Make sure this completes without errors.

        cd $GUMSHOE_HOME
        mvn install
    
Step 2: Get target app
----------------------

Thanks to Chris Hallson for writing this example (and probably forgetting all about it years ago).
We'll use my fork of it to make it easier to add options to our java commandline.

1.  Get the chat program.

    Download and unpack JavaChat source.

        wget https://github.com/newbrough/JavaChat/archive/master.zip
        unzip JavaChat-master.zip
        cd JavaChat-master
        export CHAT_HOME=`pwd` # maybe put this in .bashrc too?

2.  Try it out.

    Before looking at the I/O with gumshoe, lets just give this app a try.
    Open two shell windows and in each one, run:

        cd $CHAT_HOME
        ./start-chat.sh
    
    You should have two identical chat windows.  One of them select "server" and click "Connect".
    The other leave as "client" and click "Connect".  They should both show messages that they are connected.
    Change the name of both to something other than "Unknown".  Try sending a message or two.

    Everything working ok?  Great!

Step 3: Lets see it already
---------------------------

1.  Run with gumshoe

    Again we will use two terminal windows and in one run the chat program as before.  
    But in one of those terminals, start the chat program with:

        ./start-chat.sh --gumshoe
    
    You should see two windows now -- one is the normal chat program, and the other is the gumshoe viewer.

2.  Make something to look at.

    Connect one chat window as server, the other as client.  Do something so there is some I/O to examine.
    Change the name, send some messages, whatever.

3.  Take a snapshot.

    In the gumshoe window, navigate to the "Collect" tab.
    After 30seconds of I/O (default settings) you should see "No data received" replaced by the time of the last sample sent by the probe.
    Once you see that, click "Update" to view the latest sample received.  You should see some blocks appear in the top main portion of the window.

4.  What is this thing?

    The default graph is a root graph -- the top of the stack immediately causing the I/O is shown on top,
    the callers that invoked those methods next, and so on down the stack.

    For example, along the top you may see boxes for Socket.read() and Socket.write().
    Below Socket.read() may be 3 different boxes that are each a method that called Socket.read().
    The width of each box may represent the proportion of I/O.
    Boxes colored red represent a frame responsible for 50% or more of displayed I/O, yellow is 25% or more.  

    The box width currently represents the number of read operations, although bytes, operations and elapsed time
    are tracked for reads and writes.  Hover over a box to view all values.

5.  Just like that, but different.

    Navigate to the "Display" tab.  Try changing the graph settings.  Click "Apply" to view the settings in the current graph.

    Operation and measurement choices change which values are used to render width, color and choose which stack frames to view.

    The Direction setting lets you choose either a flame graph or root graph.  
    A flame graph starts at code entry points at bottom of stack,
    and can help identify some upstream triggers of I/O like _which of my REST services result in the most I/O_?
    A root graph starts at proximal cause of I/O at top of stack,
    and can help identify lower-level bottlenecks like _is my database or REST client responsible for more I/O_?

    The default vuew uses the raw value for cell width, 
    so if a box is twice as wide as another then that stack frame is involved in twice as much I/O
    (read operations, write milliseconds or whichever type happens to be selected at the moment).
    To see frames that may be too narrow to appear otherwise, switch to log(value) or equal width sizing.   

    Finally, arrange by value sorts cells so those with the most I/O appear on the left.  
    Note this may be confusing when changing other display options as the relative positions will move around more.

    Navigate to the "Examine" tab and click on a cell.  
    This shows all the statistics accumulated for that stack frame and its relation to the parent frame in the graph.

6.  Keep it real. 
 
    All the stacks seen so far make great examples of how we can navigate the display,
    but most of what is visible by default is irrelevant.  
    That was intentional (by the defaults in start-chat.sh) so we could use a small, simple program for this demo.

    When using gumshoe with a real project, however, 
    you are probably only interested in I/O related to parts of the program under your control.
    This is what filters are all about.

    Navigate to the "Filter" tab, check the "drop JDK and gumshoe frames" and click "Apply to display".  
    Immediately all the ObjectInputStream and Socket stack frames are gone and you are left with just the
    things in the javachat application and its libraries.
    This doesn't look nearly as cool, because javachat is a pretty simple application.  
    (Which is why we poked around the display options with the full stack instead.)

    Here you can add (fully-qualified) packages or classes to look for or exclude from analysis.
    Click "Apply to display" to filter the stacks just for your view.
    Click "Apply to probe" to drop unneeded stack frames from the initial collection,
    which reduces the resource usage and overhead of the gumshoe probe.

    Early analysis can also benefit from reducing stacks down to just the top and bottom few frames.
    For example, the original target application for gumshoe 
    had threads that began with a REST, SOAP, or timer kicking off some action,
    then filter down through various layers of business logic,
    finally resulting in a SQL call, a direct TCP socket to another system, 
    or making a REST call to an external system.  
    Limiting the view alternately to the top or bottom few frames
    showed the relative cost of services we provided and services we called,
    and gave good targets for later filters to probe the full stack of those specific bottlenecks.

Step 4: Now what?
-----------------

1.  Try it with your app 

    The original javachat java cmdline was:

        java -classpath dist/JavaChat.jar:lib/* javachat.JavaChat
 
    To run with gumshoe, several options and arguments were added.  Specifically:

    * Add hooks to bootclasspath

        -Xbootclasspath/p:$GUMSHOE_HOME/gumshoe-hooks/target/gumshoe-hooks-0.1.0-SNAPSHOT.jar

    * Add gumshoe-probes and gumshoe-tools to normal classpath

    * Insert com.dell.gumshoe.tools.Gumshoe as the main class, make the original main class the first argument 

    * System properties set initial filter and reporting time, but you probably don't want these.

        The default is to report every 5min and automatically filter out the JDK and gumshoe classes,
        which is probably appropriate.  We reported every 30sec in the javachat example just so you could
        see some data quickly, and filtered nothing out so there was more to see.
    
        You may want to select just the classes from your project.  Maybe select your project com.mycompany.proj
        and a library org.thirdparty.mylib using the property:
    
            -Dgumshoe.socket-io.include=com.mycompany.proj,org.thirdparty.mylib
        
