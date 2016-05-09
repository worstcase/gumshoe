package sun.misc;

import com.dell.gumshoe.hook.IoTraceHandler;

import java.net.InetAddress;

/** redefine template from rt.jar
 *
 *  do as little work as possible here -- define a proper interface and way to set it,
 *  then do any real work outside of hacking sun.misc package
 */
public final class IoTrace {
    private IoTrace() { }
    /**
     * Called before data is read from a socket.
     *
     * @return a context object
     */
    public static Object socketReadBegin() {
        return IoTraceHandler.socketReadBegin();
    }

    /**
     * Called after data is read from the socket.
     *
     * @param context
     *            the context returned by the previous call to socketReadBegin()
     * @param address
     *            the remote address the socket is bound to
     * @param port
     *            the remote port the socket is bound to
     * @param timeout
     *            the SO_TIMEOUT value of the socket (in milliseconds) or 0 if
     *            there is no timeout set
     * @param bytesRead
     *            the number of bytes read from the socket, 0 if there was an
     *            error reading from the socket
     */
    public static void socketReadEnd(Object context, InetAddress address, int port, int timeout, long bytesRead) {
        IoTraceHandler.socketReadEnd(context, address, port, timeout, bytesRead);
    }

    /**
     * Called before data is written to a socket.
     *
     * @return a context object
     */
    public static Object socketWriteBegin() {
        return IoTraceHandler.socketWriteBegin();
    }

    /**
     * Called after data is written to a socket.
     *
     * @param context
     *            the context returned by the previous call to
     *            socketWriteBegin()
     * @param address
     *            the remote address the socket is bound to
     * @param port
     *            the remote port the socket is bound to
     * @param bytesWritten
     *            the number of bytes written to the socket, 0 if there was an
     *            error writing to the socket
     */
    public static void socketWriteEnd(Object context, InetAddress address, int port, long bytesWritten) {
        IoTraceHandler.socketWriteEnd(context, address, port, bytesWritten);
    }

    /**
     * Called before data is read from a file.
     *
     * @param path
     *            the path of the file
     * @return a context object
     */
    public static Object fileReadBegin(String path) {
        return IoTraceHandler.fileReadBegin(path);
    }

    /**
     * Called after data is read from a file.
     *
     * @param context
     *            the context returned by the previous call to fileReadBegin()
     * @param bytesRead
     *            the number of bytes written to the file, 0 if there was an
     *            error writing to the file
     */
    public static void fileReadEnd(Object context, long bytesRead) {
        IoTraceHandler.fileReadEnd(context, bytesRead);
    }

    /**
     * Called before data is written to a file.
     *
     * @param path
     *            the path of the file
     * @return a context object
     */
    public static Object fileWriteBegin(String path) {
        return IoTraceHandler.fileWriteBegin(path);
    }

    /**
     * Called after data is written to a file.
     *
     * @param context
     *            the context returned by the previous call to fileReadBegin()
     * @param bytesWritten
     *            the number of bytes written to the file, 0 if there was an
     *            error writing to the file
     */
    public static void fileWriteEnd(Object context, long bytesWritten) {
        IoTraceHandler.fileWriteEnd(context, bytesWritten);
    }
}
