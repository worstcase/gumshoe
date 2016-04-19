package com.dell.gumshoe.file.io;

import com.dell.gumshoe.io.IOEvent;

public abstract class FileIOEvent extends IOEvent {
    public FileIOEvent(String path) {
        setTarget(path);
    }
}
