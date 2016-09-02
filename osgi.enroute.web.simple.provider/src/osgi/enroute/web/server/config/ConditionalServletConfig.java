package osgi.enroute.web.server.config;

import osgi.enroute.servlet.api.ConditionalServlet;

public @interface ConditionalServletConfig {
	/**
	 * Timeout of the blacklist, in milliseconds.
	 * 
	 * If a {@link ConditionalServlet} behaves badly and throws an Exception, it will
	 * be blacklisted. Use this configuration property to configure the amount of time
	 * that such servlets will be blacklisted.
	 * 
	 * A value of 0 means that nothing gets blacklisted. A value of -1 means that any
	 * badly behaving servlet will be blacklisted "forever" (i.e for the entire lifecycle
	 * of the provider.
	 * 
	 * Default value is 300000 (5 minutes).
	 */
	 long timeout() default 300000;
}
