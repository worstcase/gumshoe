package com.dell.gumshoe.file;

import com.dell.gumshoe.io.IOAccumulator;
import com.dell.gumshoe.stack.StackFilter;

/** create hierarchical analysis of socket IO versus stack
 *  with total IO at each frame and
 *  link from frame to multiple next frames below
 */
public class FileIOAccumulator extends IOAccumulator<FileIODetailAdder> {

    public FileIOAccumulator(StackFilter filter) {
        super(filter);
    }

    @Override
    public FileIODetailAdder newAdder() {
        return new FileIODetailAdder();
    }

}
