errcode
=======

A small Ratpack app, which downloads log files from remote servers (via SCP and Samba), extracts stack traces based 
on a specific string pattern, processes them and stores them in a MySQL database. 

Each stack trace is associated with a unique error code (a.k.a. `errcode`). So, you can query the database for a specific
error code and get back the corresponding stack trace details via a simple and intuitive web interface.

The main use case is to efficiently troubleshoot problems reported by the users of another application (say, `app-b`). 
That is, when something wrong happens in `app-b`, the end user gets a message along with an error code, 
which can then be quoted in a possible communication with the technical support of `app-b`. The technical support can utilise 
the `errcode` app to search for the reported code (provided that `errcode` has been configured to process `app-b`'s log files), 
quickly pinpoint the root cause of the problem and swiftly reply to the end user.

### TODOs
* Parallelise log file downloads.
* Implement on-demand indexing of stack traces. Currently, the app can only _periodically_ download and process log files, based on
an externally configured scheduled job. So, it is possible to search for an error code which hasn't been stored in the database yet
and get back zero results. When this happens, it would be better if the app could search log files directly on demand
and extract and process the stack trace in question in real time.
* Instead of downloading and processing entire log files (pull approach), consider implementing a more efficient push approach
where an agent of some sorts (e.g. [logstash-forwarder](https://github.com/elasticsearch/logstash-forwarder)) constantly
monitors the log files for changes and sends a properly formatted message to the `errcode` API whenever a new error code
is detected. In that case, both of the two points above will not be necessary anymore.
