package osgi.enroute.rest.openapi.api;

import java.net.URI;
import java.util.Map;

import org.osgi.dto.DTO;

import osgi.enroute.rest.openapi.annotations.Required;

/**
 * Allows the definition of a security scheme that can be used by the
 * operations. Supported schemes are basic authentication, an API key
 * (either as a header or as a query parameter) and OAuth2's common flows
 * (implicit, password, application and access code).
 * 
 * 
 */
public class SecuritySchemeObject extends DTO {
	/**
	 * string Any Required. The type of the security scheme. Valid values
	 * are "basic", "apiKey" or "oauth2".
	 */
	@Required
	public SecurityScheme		type;
	/**
	 * string Any A short description for security scheme.
	 */
	public String				description;
	/**
	 * string apiKey Required. The name of the header or query parameter to
	 * be used.
	 */
	@Required
	public String				name;

	/**
	 * string apiKey Required The location of the API key. Valid values are
	 * "query" or "header".
	 */
	@Required
	public In					in;

	/**
	 * auth2 Required. The flow used by the OAuth2 security scheme. Valid
	 * values are "implicit", "password", "application" or "accessCode".
	 */
	public OAuth2Flow			flow;

	/**
	 * string oauth2 ("implicit", "accessCode") Required. The authorization
	 * URL to be used for this flow. This SHOULD be in the form of a URL.
	 */
	@Required
	public URI					authorizationUrl;

	/**
	 * string oauth2 ("password", "application", "accessCode") Required. The
	 * token URL to be used for this flow. This SHOULD be in the form of a
	 * URL.
	 */
	@Required
	public URI					tokenUrl;

	/**
	 * Scopes Object oauth2 Required. The available scopes for the OAuth2
	 * security scheme.
	 */
	@Required
	public Map<String, String>	scopes;
}
