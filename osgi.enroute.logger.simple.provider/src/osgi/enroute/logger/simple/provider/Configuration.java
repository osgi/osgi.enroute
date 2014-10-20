package osgi.enroute.logger.simple.provider;

import osgi.enroute.logger.api.Level;
import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Define the configuraiton of the Logger Admin.
 */
@OCD(description = "The Logger Admin provides runtime control of the that come from SLF4J and java.util.logging logging")
interface Configuration {

	@AD(description = "The minimum level to log", deflt = "WARN", required = false)
	Level level();

	@AD(description = "Print stack traces when an exception occurs", deflt = "false", required=false)
	boolean traces();

	@AD(description = "Provide the location of the caller method in the log.", deflt = "false", required=false)
	boolean where();

	@AD(description= "Use the log output from java.util.logging", required=false)
	boolean javaUtilLogging();
}
