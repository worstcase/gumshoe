package com.dell.gumshoe;

import com.dell.gumshoe.socket.io.SocketIOMonitor;
import com.dell.gumshoe.socket.unclosed.SocketCloseMonitor;
import com.dell.gumshoe.socket.unclosed.SocketCloseMonitor.SocketImplDecorator;
import com.dell.gumshoe.stack.Filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

/** create simple TCP client and server to test out the socket diag tools */
public class TestSocketDiag extends TestCase {
    static String testMessage = "now is the time for all good men to come to the aid of their king";
    static byte[] testData = testMessage.getBytes();

    SocketCloseMonitor target;

    @Override
    public void setUp() throws Exception {
        target = new SocketCloseMonitor(0, Filter.NONE);
        Socket.setSocketImplFactory(target);
        final SocketIOMonitor ioMonitor = new SocketIOMonitor();
//        ioMonitor.addListener(new SocketIOMonitor.PrintEvents());
//        IoTrace.setDelegate(ioMonitor);
    }

    public void testWithClientServer() throws Exception {
        // client just closes socket right away, but server will leave it open for 1sec
        EchoServer server = startServer();
        executeClient(server);

        // see if we can find the open socket
        List<SocketImplDecorator> sockets = target.findOpenedBefore(new Date());
        assertEquals(1, sockets.size());

        // look in stack and see its from the server
        String stackLines = sockets.get(0).toString();
        assertTrue(stackLines.contains(EchoServer.class.getSimpleName()));

        // TODO: use a better sync approach than sleep
        Thread.sleep(1500);

        // now server socket has closed, should be none left open
        sockets = target.findOpenedBefore(new Date());
        assertEquals(0, sockets.size());
    }

    /////

    private EchoServer startServer() {
        EchoServer server = new EchoServer();
        new Thread(server).start();
        return server;
    }

    private void executeClient(EchoServer server) throws InterruptedException, UnknownHostException, IOException {
        while(server.port==0) {
            Thread.sleep(100);
        }
        System.out.println("using port " + server.port);
        Socket socket = new Socket("127.0.0.1", server.port);

        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        System.out.println("client writing");
        out.write(testData);
        out.flush();
        socket.shutdownOutput();
        System.out.println("client wrote");

        StringBuilder buffer = new StringBuilder();
        int inByte;
        while((inByte=in.read())!=-1) {
            buffer.append((char)inByte);
        }

        String received = buffer.toString();
        System.out.println("client read: " + received);
        System.out.println("client closing");
        socket.close();
    }

    private static class EchoServer implements Runnable {
        private int port;
        private String received;
        private Exception thrown;
        @Override
        public void run() {
            try {
                runWithExceptions();
            } catch(Exception e) {
                e.printStackTrace();
                thrown = e;
            }

        }

        private void runWithExceptions() throws Exception {
            ServerSocket ss = new ServerSocket(0);
            port = ss.getLocalPort();
            Socket socket = ss.accept();

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            System.out.println("server reading, avail = " + in.available());
            StringBuilder buffer = new StringBuilder();
            int inByte;
            while((inByte=in.read())==-1) {
                Thread.sleep(100);
            }
            buffer.append((char)inByte);
            while((inByte=in.read())!=-1) {
                buffer.append((char)inByte);
            }
            received = buffer.toString();
            System.out.println("server read: " + received);

            out.write(received.getBytes());
            socket.shutdownOutput();
            System.out.println("server wrote");

            Thread.sleep(1000);
            socket.close();
            ss.close();

            System.out.println("server closed");
        }
    }
}
