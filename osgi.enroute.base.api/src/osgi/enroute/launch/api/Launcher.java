package osgi.enroute.launch.api;

/**
 * A launcher is a program that starts an OSGi framework and manages the set of
 * installed bundles.
 * <p>
 * This service will be registered when the launcher has started all the
 * bundles. It will contain a property {@link #LAUNCHER_MAIN} that contains the
 * command line arguments as a String[].
 */
public interface Launcher {
	/**
	 * This property contains a String[] for the command line arguments.
	 */
	String	LAUNCHER_MAIN	= "osgi.launcher.main";
}
