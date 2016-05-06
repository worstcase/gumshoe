package com.dell.gumshoe.file;

import com.dell.gumshoe.hook.IoTraceListener.FileListener;
import com.dell.gumshoe.io.IOMonitor;

/** monitor socket IO and report each as an event to registered listeners
 */
public class FileIOMonitor extends IOMonitor implements FileListener {
    private final FileMatcher fileFilter;

    public FileIOMonitor() {
        this(FileMatcher.ANY, 500, Thread.MIN_PRIORITY, 1, false);
    }

    public FileIOMonitor(FileMatcher fileFilter, int queueSize, int priority, int count, boolean statsEnabled) {
        this.fileFilter = fileFilter;
        setEventQueueSize(queueSize);
        setThreadCount(count);
        setThreadPriority(priority);
        setQueueStatisticsEnabled(statsEnabled);
    }

    @Override
    public Object fileReadBegin(String path) {
        return fileFilter.matches(path) ? FileReadEvent.begin(path) : null;
    }

    @Override
    public Object fileWriteBegin(String path) {
        return fileFilter.matches(path) ? FileWriteEvent.begin(path) : null;
    }

    @Override
    public void fileReadEnd(Object context, long bytesRead) {
        handleEvent(context, bytesRead);
    }

    @Override
    public void fileWriteEnd(Object context, long bytesWritten) {
        handleEvent(context, bytesWritten);
    }

    private void handleEvent(Object context, long bytes) {
        if(context!=null) {
            final FileIOEvent operation = (FileIOEvent)context;
            operation.complete(bytes);
            queueEvent(operation);
        }
    }
}
