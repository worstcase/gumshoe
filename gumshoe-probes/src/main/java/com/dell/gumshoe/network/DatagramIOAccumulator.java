package com.dell.gumshoe.network;

import com.dell.gumshoe.io.IOAccumulator;
import com.dell.gumshoe.stack.StackFilter;

/** create hierarchical analysis of socket IO versus stack
 *  with total IO at each frame and
 *  link from frame to multiple next frames below
 */
public class DatagramIOAccumulator extends IOAccumulator<DatagramIODetailAdder> {

    public DatagramIOAccumulator(StackFilter filter) {
        super(filter);
    }

    @Override
    public DatagramIODetailAdder newAdder() {
        return new DatagramIODetailAdder();
    }

}
