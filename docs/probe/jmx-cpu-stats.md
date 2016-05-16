MBean Management of CPU Stats Reporting
=======================================

By default if CPU statistics reporting is enabled, an MBean will be also be installed.
Properties used to initialize gumshoe can change this behavior and either force or disable
installation of the MBean or assign a specific name to the bean installed.

This mbean will allow you to alter these attributes:

    Enabled                 Enable collection of CPU usage statistics (true or false)
    ReportingFrequency      How often statistics are reported to configured listeners (milliseconds)
    ShutdownReportEnabled   Enable the shutdown hook that sends the final report to configured listeners  (true or false)
    DumpInterval            How frequently to collect thread statistics (milliseconds)
    EffectiveInterval       (read only) Gumshoe will try to collect statistics at DumpInterval,
                            but it may adjust the collection rate dynamically if needed.  This
                            is the current collection frequency (milliseconds).
    AverageDumpTime         How long the collection process takes, average over all collections performed. (milliseconds)
    ThreadPriority          Collection is performed asynchronously on a thread with this priority (default Thread.MIN_PRIORITY)

In addition these operations can be performed:

    getReport()             Return a text report of the current contents of the collection buffer.
                            This will likely represent a partial report if periodic reporting is enabled
                            for the time since the start of the last reporting interval.
    reset()                 Clear the contents of the collection buffer.
                            If periodic reporting is enabled, the next report sent to configured listeners
                            will not contain a full report as this data will have been removed.