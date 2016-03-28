package com.dell.gumshoe.stack;

import junit.framework.TestCase;

public class TestStackFilter extends TestCase {
    private StackFilter EXCLUDE_BUILTIN = Filter.builder().withExcludePlatform().build();
    
    public String testCase1 = "com.dell.gumshoe.socket.Stack.<init>(Stack.java:11)\n" +
            "com.dell.gumshoe.socket.SocketIOMonitor$Event.<init>(SocketIOMonitor.java:91)\n" +
            "com.dell.gumshoe.socket.SocketIOMonitor.socketWriteBegin(SocketIOMonitor.java:64)\n" +
            "sun.misc.IoTrace.socketWriteBegin(IoTrace.java:56)\n" +
            "java.net.SocketOutputStream.socketWrite(SocketOutputStream.java:109)\n" +
            "java.net.SocketOutputStream.write(SocketOutputStream.java:159)\n" +
            "java.io.BufferedOutputStream.flushBuffer(BufferedOutputStream.java:82)\n" +
            "java.io.BufferedOutputStream.flush(BufferedOutputStream.java:140)\n" +
            "com.mysql.jdbc.MysqlIO.send(MysqlIO.java:3832)\n" +
            "com.mysql.jdbc.MysqlIO.quit(MysqlIO.java:2196)\n" +
            "com.mysql.jdbc.ConnectionImpl.realClose(ConnectionImpl.java:4446)\n" +
            "com.mysql.jdbc.ConnectionImpl.close(ConnectionImpl.java:1594)\n" +
            "org.apache.tomcat.jdbc.pool.PooledConnection.disconnect(PooledConnection.java:331)\n" +
            "org.apache.tomcat.jdbc.pool.PooledConnection.release(PooledConnection.java:490)\n" +
            "org.apache.tomcat.jdbc.pool.ConnectionPool.release(ConnectionPool.java:581)\n" +
            "org.apache.tomcat.jdbc.pool.ConnectionPool.checkIdle(ConnectionPool.java:1002)\n" +
            "org.apache.tomcat.jdbc.pool.ConnectionPool.checkIdle(ConnectionPool.java:983)\n" +
            "org.apache.tomcat.jdbc.pool.ConnectionPool$PoolCleaner.run(ConnectionPool.java:1350)\n" +
            "java.util.TimerThread.mainLoop(Timer.java:555)\n" +
            "java.util.TimerThread.run(Timer.java:505)";

    public String testCase2 = "com.dell.gumshoe.stack.Stack.<init>(Stack.java:9)\n" + 
            "com.dell.gumshoe.socket.SocketIOMonitor$Event.<init>(SocketIOMonitor.java:92)\n" + 
            "com.dell.gumshoe.socket.SocketIOMonitor.socketReadBegin(SocketIOMonitor.java:60)\n" + 
            "sun.misc.IoTrace.socketReadBegin(IoTrace.java:27)\n" + 
            "java.net.SocketInputStream.read(SocketInputStream.java:148)\n" + 
            "java.net.SocketInputStream.read(SocketInputStream.java:122)\n" + 
            "com.mysql.jdbc.util.ReadAheadInputStream.fill(ReadAheadInputStream.java:114)\n" + 
            "com.mysql.jdbc.util.ReadAheadInputStream.readFromUnderlyingStreamIfNecessary(ReadAheadInputStream.java:161)\n" + 
            "com.mysql.jdbc.util.ReadAheadInputStream.read(ReadAheadInputStream.java:189)\n" + 
            "com.mysql.jdbc.MysqlIO.readFully(MysqlIO.java:3036)\n" + 
            "com.mysql.jdbc.MysqlIO.readPacket(MysqlIO.java:592)\n" + 
            "com.mysql.jdbc.MysqlIO.doHandshake(MysqlIO.java:1078)\n" + 
            "com.mysql.jdbc.ConnectionImpl.coreConnect(ConnectionImpl.java:2412)\n" + 
            "com.mysql.jdbc.ConnectionImpl.connectWithRetries(ConnectionImpl.java:2253)\n" + 
            "com.mysql.jdbc.ConnectionImpl.createNewIO(ConnectionImpl.java:2235)\n" + 
            "com.mysql.jdbc.ConnectionImpl.<init>(ConnectionImpl.java:813)\n" + 
            "com.mysql.jdbc.JDBC4Connection.<init>(JDBC4Connection.java:47)\n" + 
            "sun.reflect.GeneratedConstructorAccessor7.newInstance(Unknown Source)\n" + 
            "sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)\n" + 
            "java.lang.reflect.Constructor.newInstance(Constructor.java:526)\n" + 
            "com.mysql.jdbc.Util.handleNewInstance(Util.java:411)\n" + 
            "com.mysql.jdbc.ConnectionImpl.getInstance(ConnectionImpl.java:399)\n" + 
            "com.mysql.jdbc.NonRegisteringDriver.connect(NonRegisteringDriver.java:334)\n" + 
            "org.apache.tomcat.jdbc.pool.PooledConnection.connectUsingDriver(PooledConnection.java:278)\n" + 
            "org.apache.tomcat.jdbc.pool.PooledConnection.connect(PooledConnection.java:182)\n" + 
            "org.apache.tomcat.jdbc.pool.ConnectionPool.createConnection(ConnectionPool.java:702)\n" + 
            "org.apache.tomcat.jdbc.pool.ConnectionPool.borrowConnection(ConnectionPool.java:634)\n" + 
            "org.apache.tomcat.jdbc.pool.ConnectionPool.getConnection(ConnectionPool.java:188)\n" + 
            "org.apache.tomcat.jdbc.pool.DataSourceProxy.getConnection(DataSourceProxy.java:128)\n" + 
            "org.dell.persist.Transaction.open(Transaction.java:573)\n" + 
            "org.dell.persist.Transaction.execute(Transaction.java:423)\n" + 
            "org.dell.persist.RelationalCache.load(RelationalCache.java:494)\n" + 
            "org.dell.persist.RelationalCache.find(RelationalCache.java:306)\n" + 
            "com.dell.provisioning.cloud.CloudFactory.getServers(CloudFactory.java:2266)\n" + 
            "com.dell.cloud.analytics2.ServerAnalyticsWorker.checkCloud(ServerAnalyticsWorker.java:75)\n" + 
            "com.dell.monitor.AccountResourceMonitor._run(AccountResourceMonitor.java:294)\n" + 
            "com.dell.monitor.AccountResourceMonitor.run(AccountResourceMonitor.java:220)\n" + 
            "java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:471)\n" + 
            "java.util.concurrent.FutureTask.run(FutureTask.java:262)\n" + 
            "java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)\n" + 
            "java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)\n" + 
            "java.lang.Thread.run(Thread.java:745)";


    private Stack parse(String value) {
        String[] lines = value.split("\n");
        StackTraceElement[] frames = new StackTraceElement[lines.length];
        for(int i=0;i<lines.length;i++) {
            try {
                String[] fields = lines[i].split("[(:)]");
                String classAndMethod = fields[0];
                int position = classAndMethod.lastIndexOf('.');
                String className = classAndMethod.substring(0, position);
                String methodName = classAndMethod.substring(position+1);
                String file = fields[1];
                String line = fields.length==2 ? "0" : fields[2];
                frames[i] = new StackTraceElement(className, methodName, file, Integer.parseInt(line));
            } catch(RuntimeException e) {
                System.out.println("could not parse: " + lines[i]);
                throw e;
            }
            
        }
        return new Stack(frames);
    }
    
    public void testLogfileExample() {
        Stack stack = parse(testCase1);
        
        final String unfiltered = stack.toString();
        System.out.println("unfiltered:\n" + unfiltered);
        assertTrue(unfiltered.contains("at java."));
        assertTrue(unfiltered.contains("at com.dell.gumshoe."));
        
        final String filtered = stack.applyFilter(EXCLUDE_BUILTIN).toString();
        System.out.println("\nfiltered:\n" + filtered);
        assertFalse(filtered.contains("at java."));
        assertFalse(filtered.contains("at com.dell.gumshoe."));
    }
    
    public void testLogfileExample2() {
        Stack stack = parse(testCase2);
        
        final String unfiltered = stack.toString();
        System.out.println("unfiltered:\n" + unfiltered);
        assertTrue(unfiltered.contains("at java."));
        assertTrue(unfiltered.contains("at com.dell.gumshoe."));
        
        final String filtered = stack.applyFilter(EXCLUDE_BUILTIN).toString();
        System.out.println("\nfiltered:\n" + filtered);
        assertFalse(filtered.contains("at java."));
        assertFalse(filtered.contains("at com.dell.gumshoe."));
    }
}
