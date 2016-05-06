# OSGI ENROUTE LOGREADER ROLLING PROVIDER

Implements a Log Reader that stores the logs on disks and rolls them over.

## Configuration

The component is providing metatype so the configuration can be edited in WebConsole.

	Pid: osgi.enroute.logreader.rolling
	
	Field		 Type		Default			Description
	where		 String		"messages"		Either abs file name or 
											relative to bundle data area
	logSize		 int		100				Nr of Kilobytes per log
	numberOfLogs int		10				Nr of logs to keep
	level        LogLevel   DEBUG			Up to what level to store	
	format		String		"%s %8s %s%n" 	Format string
		
## Commands

* error – Generate an error for testing
* logfiles – Show the current log files


