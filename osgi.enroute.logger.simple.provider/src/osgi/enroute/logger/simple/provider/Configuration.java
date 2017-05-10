package osgi.enroute.logger.simple.provider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import osgi.enroute.logger.api.Level;

/**
 * Define the configuration of the Logger Admin.
 */
@ObjectClassDefinition(description = "The Logger Admin provides runtime control of the that come from SLF4J and java.util.logging logging")
@interface Configuration {

	@AttributeDefinition(description = "The minimum level to log", required = false)
	Level level() default Level.WARN;

	@AttributeDefinition(description = "Print stack traces when an exception occurs", required = false)
	boolean traces() default false;

	@AttributeDefinition(description = "Provide the location of the caller method in the log.", required = false)
	boolean where() default false;

	@AttributeDefinition(description = "Use the log output from java.util.logging", required = false)
	boolean javaUtilLogging() default false;
}
