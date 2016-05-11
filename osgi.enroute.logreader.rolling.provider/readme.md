# OSGI ENROUTE LOGREADER ROLLING PROVIDER

Implements a Log Reader that stores the logs on disks and rolls them over.

## Configuration

The component is providing metatype so the configuration can be edited in WebConsole.

Pid: osgi.enroute.logreader.rolling

| Field           	| Type     	| Default       	| Description                                               	|
|-----------------	|----------	|---------------	|-----------------------------------------------------------	|
| root            	| String   	| "messages"    	| Either absolute file name or relative to bundle data area 	|
| maxLogSizeMb    	| int      	| 1             	| Number of Megabytes per log                               	|
| maxRetainedLogs 	| int      	| 10            	| Number of logs to retain                                  	|
| level           	| LogLevel 	| DEBUG         	| Up to what level to store                                 	|
| format          	| String   	| "%s %8s %s%n" 	| Format string                                             	|
		
## Commands

* error – Generate an error message for testing
* warn  - Generate a warning message for testing
* info  - Generate an informational message for testing
* debug - Generate a debug message for testing
* logfiles – Show the current log files