package {{basePackageName}};

import org.osgi.annotation.versioning.ProviderType;

/**
 * This is an example OSGi enRoute bundle that has a component that implements an
 * API.
 */

@ProviderType
public interface {{service}} {

	/**
	 * The interface is a minimal method.
	 *
	 * @param message the message to say
	 * @return true if the message could be spoken
	 */
	boolean say(String message);

}
