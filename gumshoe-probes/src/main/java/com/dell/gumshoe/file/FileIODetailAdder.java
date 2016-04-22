package com.dell.gumshoe.file;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.io.IOEvent;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.StatisticAdder;

public class FileIODetailAdder extends IODetailAdder {
    @Override
    public String getType() {
        return ProbeManager.FILE_IO_LABEL;
    }

    @Override
    public StatisticAdder<IOEvent> newInstance() {
        return new FileIODetailAdder();
    }

    public static FileIODetailAdder fromString(String line) {
        final FileIODetailAdder out = new FileIODetailAdder();
        try {
            out.setFromString(line);
        } catch(Exception e) {
            return null;
        }
        return out;
    }
}