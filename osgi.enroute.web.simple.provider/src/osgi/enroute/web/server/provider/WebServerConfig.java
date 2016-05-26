package osgi.enroute.web.server.provider;

public @interface WebServerConfig {
	String osgi_http_whiteboard_servlet_pattern();

	boolean noBundles();

	String[]directories() default {};

	int expires();

	boolean exceptions();

	boolean debug();

	boolean noproxy();

	long expiration();

	int maxConnections();

	String maxConnectionMessage();

	int maxTime();

	String maxTimeMessage();
}
