package osgi.enroute.logging.api;

import java.util.concurrent.Callable;

import org.osgi.annotation.versioning.ProviderType;


@ProviderType
public interface Logger extends org.slf4j.Logger {
	interface TRACE {}

	interface INFO {}

	interface DEBUG {}

	interface WARN {}

	interface ERROR {}

	<T extends Logger> T scoped(Class<T> type, String prefix);
	
	Runnable wrap( Runnable r, String msg);
	<T> Callable<T> wrap( Callable<T> c, String msg);
}