package com.dell.gumshoe.network;

import com.dell.gumshoe.ProbeManager;
import com.dell.gumshoe.io.IOEvent;
import com.dell.gumshoe.stats.IODetailAdder;
import com.dell.gumshoe.stats.StatisticAdder;

public class DatagramIODetailAdder extends IODetailAdder {
    @Override
    public String getType() {
        return ProbeManager.DATAGRAM_IO_LABEL;
    }

    @Override
    public StatisticAdder<IOEvent> newInstance() {
        return new DatagramIODetailAdder();
    }

    public static DatagramIODetailAdder fromString(String line) {
        final DatagramIODetailAdder out = new DatagramIODetailAdder();
        try {
            out.setFromString(line);
        } catch(Exception e) {
            return null;
        }
        return out;
    }

}