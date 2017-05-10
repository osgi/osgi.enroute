# OSGI ENROUTE EXECUTOR SIMPLE PROVIDER

${Bundle-Description}

## Example

## Configuration

	Pid: osgi.executor.provider
	
	Field					Type				Description
	coreSize				int					The minimum number of threads 
												allocated to this pool
	maximumPoolSize								Maximum number of threads 
												allocated to this pool
	keepAliveTime			long				Nr of seconds an idle free 
												thread should survive before being 
												destroyed
	ranking					int					service ranking (default -1000)
		
## References

