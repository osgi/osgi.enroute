package osgi.enroute.web.server.config;

public @interface WebServerConfig {

	boolean debug();

	int expires();

	long expiration();

	boolean noproxy();

	boolean addTrailingSlash();
}
