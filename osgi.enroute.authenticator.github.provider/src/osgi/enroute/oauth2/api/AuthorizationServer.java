package osgi.enroute.oauth2.api;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.Set;

import org.osgi.util.promise.Promise;

public interface AuthorizationServer {
	final static String DOMAIN = "domain";
	
	interface AccessToken {
		HttpURLConnection authorize( URI resource ) throws Exception;
		HttpURLConnection authorize( URI resource, Proxy proxy ) throws Exception;
		Set<String> getScopes();
	}
	
	String getClientId();
	
	URI getAuthorizationEndpoint(String name, URI success, URI fail, String ... scope) throws Exception;
	Promise<AccessToken> getAccessToken(String name) throws Exception;
	Promise<AccessToken> getAccessToken(String ... scope) throws Exception;
}
