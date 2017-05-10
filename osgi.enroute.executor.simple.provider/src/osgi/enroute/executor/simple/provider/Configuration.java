package osgi.enroute.executor.simple.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import aQute.bnd.annotation.metatype.Meta.AD;

@ObjectClassDefinition(description = "Configuration for the enRoute::Executor")
public @interface Configuration {
	
	@AD(description = "The minimum number of threads allocated to this pool", deflt = "20", required=false)
	int coreSize();

	@AD(description = "Maximum number of threads allocated to this pool", deflt = "0", required=false)
	int maximumPoolSize();

	@AD(description = "Nr of seconds an idle free thread should survive before being destroyed", deflt = "60", required=false)
	long keepAliveTime();
	
	@AD(description= "Ranking", deflt="-1000", required=false)
	long ranking(); 
}
