# OSGI ENROUTE SCHEDULER SIMPLE PROVIDER

${Bundle-Description}

## Example

Print Hello World after 1 second:

	scheduler.after( 1000 ).then( (p) -> {
	  System.out.println("Hello World");
	});

Print a message every day:

	Closeable c = scheduler.schedule( ()-> System.out.println("Hello"), "@daily" );
	...
	c.close();
	


## Configuration

	Pid: osgi.enroute.scheduler.simple
	

