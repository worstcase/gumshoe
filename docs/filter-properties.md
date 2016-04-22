Properties for Stack Filters
============================

Note on Abbreviated Property Names
----------------------------------

Each probe can have its own stack filter and each can define them using similar property names.
Property names in this section are shown with a beginning ellipsis, such as: "...filter.top"
Any probe can use this property with its specific prefix.  For example, the socket I/O probe
would use the full property name "gumshoe.socket-io.filter.top"

This section describes the individual filter settings that can be used for any probe.
For example, socket I/O monitoring will use the properties that begin "gumshoe.socket-io.filter..."
In this section we will define the various properties that can be used 

Configuration Properties
------------------------

Stacks should generally be [filtered](filters.md) reduce overhead and simplify later analysis:
                                
    ...filter.exclude-jdk    Exclude frames from java built-in packages and gumshoe 
    ...filter.include        Include only these packages or classes (comma-separated)
    ...filter.exclude        Exclude these packages or classes 
    ...filter.recursion.threshold  The recursion filter is only applied to stacks with
                             more than this number of frames (after other filters).
                             There is no default value, recursion is not applied unless set.
    ...filter.recursion.depth  Find and exclude frames performing recursion.
                             The value must be an integer length indicating the length
                             of the longest sequence of repeated frames the filter will find.
                             The default is "1", but is unused unless the threshold is set.
    ...filter.top            Number of frames at top of stack to retain
    ...filter.bottom         Number of frames at bottom of stack to retain
    ...filter.simplify       Retain only portions of the stack frames.  Allowed values are
                               NO_LINE_NUMBERS, NO_METHOD, NO_INNER_CLASSES, NO_CLASSES, NONE
                             Default is "NONE" (do not simplify frames)
    ...filter.allow-empty-stack    If filters excluded all frames from a stack,
                                                  the full unfiltered stack can be restored (if false),
                                                  or the empty stack will be used and collect stats
                                                  as an "other" category.
    ...filter.none           If true, override other filter settings: no filtering is done. 

