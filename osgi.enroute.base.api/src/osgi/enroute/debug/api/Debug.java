package osgi.enroute.debug.api;

/**
 * This service will be registered when in debug mode. The purpose of debug mode
 * is to help developers develop the system and find bugs in production. In
 * debug mode, extra bundles and services can be active to diagnose problems.
 */
public interface Debug {
	/**
	 * For shell functions on a service register the service with the
	 * {@link #COMMAND_SCOPE} and {@value #COMMAND_FUNCTION} properties. These
	 * properties are aligned with the Felix Gogo shell.
	 * <p>
	 * The scope is a {@code String} that defines the main group name of the
	 * command.
	 */
	String	COMMAND_SCOPE		= "osgi.command.scope";
	/**
	 * For shell functions on a service register the service with the
	 * {@link #COMMAND_SCOPE} and {@value #COMMAND_FUNCTION} properties. These
	 * properties are aligned with the Felix Gogo shell.
	 * <p>
	 * The function is a {@code String+} of command names. The command names
	 * must map to valid public methods on this service.
	 */
	String	COMMAND_FUNCTION	= "osgi.command.function";

	/**
	 * If this framework property is set then the framework runs a test.
	 */
	String	TEST_MODE			= "osgi.testmode";
}
