package com.dell.gumshoe.file;


public class FileReadEvent extends FileIOEvent {
    public static FileIOEvent begin(String path) {
        return new FileReadEvent(path);
    }

    private FileReadEvent(String path) {
        super(path);
    }

    @Override public boolean isRead() { return true; }
    @Override public boolean isWrite() { return false; }
}
