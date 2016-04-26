MBean Management of Socket I/O Reporting
========================================

By default if socket I/O reporting is enabled, an MBean will be also be installed.
Properties used to initialize gumshoe can change this behavior and either force or disable
installation of the MBean or assign a specific name to the bean installed.

This mbean will allow you to alter these attributes:

    Enabled                 Enable collection of socket I/O statistics (true or false)
    ReportingFrequency      How often statistics are reported to configured listeners (milliseconds)
    ShutdownReportEnabled   Enable the shutdown hook that sends the final report to configured listeners (true or false)

In addition these operations can be performed:

    getReport()             Return a text report of the current contents of the collection buffer.
                            This will likely represent a partial sample if periodic reporting is enabled
                            for the time since the start of the last reporting interval.
    reset()                 Clear the contents of the collection buffer.
                            If periodic reporting is enabled, the next report sent to configured listeners
                            will not contain a full sample as this data will have been removed.