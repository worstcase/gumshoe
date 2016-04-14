Gumshoe MBean Management of Unclosed Socket Reporting
=====================================================

By default if unclosed socket reporting is enabled, an MBean will be also be installed.
Properties used to initialize gumshoe can change this behavior and either force or disable
installation of the MBean or assign a specific name to the bean installed.

This mbean will allow you to alter these attributes:

    Enabled                 Enable collection of unclosed socket statistics (true or false)
    ClearClosedSocketsInterval Set the value N used to clear closed sockets.  Each Nth socket opened will cause
                            the system to check all tracked sockets and see if any have closed and can be  dropped.
    ReportingFrequency      How often statistics are reported to configured listeners (milliseconds)
    ShutdownReportEnabled   Enable the shutdown hook that sends the final report to configured listeners (true or false)

In addition this operation can be performed:

    getReport(age)          Return a text report of sockets currently open and having at least the given age (milliseconds).
