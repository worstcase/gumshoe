package com.dell.gumshoe.file;


public class FileWriteEvent extends FileIOEvent {
    public static FileIOEvent begin(String path) {
        return new FileWriteEvent(path);
    }

    private FileWriteEvent(String path) {
        super(path);
    }

    @Override public boolean isRead() { return false; }
    @Override public boolean isWrite() { return true; }
}
