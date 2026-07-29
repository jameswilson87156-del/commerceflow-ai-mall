# P7B PID Identity Evidence

The runtime state file is ignored at `.showcase/processes.json`. Each entry records the service name, listener PID, port, command hint, start time, and log path. New entries also record the intended working directory.

After a health check, `start.ps1` resolves the listener on the expected port and replaces the wrapper PID with the actual listener PID. This matters because Maven and npm wrappers are not necessarily the Java or Node listener.

`status.ps1` and `stop.ps1` require all of the following before treating an entry as owned: recorded PID still exists, that PID listens on the recorded port, and the command line contains the recorded command hint. A stale state entry is reported as `STALE_OR_FOREIGN` or `STOPPED`; it is not killed by name matching.

The recovery audit observed the actual listeners as Java PID `65408` on `8081`, Python PID `74128` on `8000`, Admin Node PID `27772` on `5174`, and Mobile Node PID `64864` on `5173`. These are local, transient observations, not committed configuration.
