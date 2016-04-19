package com.dell.gumshoe.file.io;

import com.dell.gumshoe.io.IOMonitor;

/** monitor socket IO and report each as an event to registered listeners
 */
public class FileIOMonitor extends IOMonitor {
    private final FileMatcher fileFilter;

    public FileIOMonitor() {
        this(FileMatcher.ANY);
    }

    public FileIOMonitor(FileMatcher fileFilter) {
        this.fileFilter = fileFilter;
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
