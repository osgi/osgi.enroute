package osgi.enroute.rest.openapi.api;

import osgi.enroute.rest.openapi.annotations.Required;

/**
 * The object provides metadata about the API. The metadata can be used by
 * the clients if needed, and can be presented in the Swagger-UI for
 * convenience.
 * 
 * <pre>
 * {
"title": "Swagger Sample App",
"description": "This is a sample server Petstore server.",
"termsOfService": "http://swagger.io/terms/",
"contact": {
"name": "API Support",
"url": "http://www.swagger.io/support",
"email": "support@swagger.io"
},
"license": {
"name": "Apache 2.0",
"url": "http://www.apache.org/licenses/LICENSE-2.0.html"
},
"version": "1.0.1"
}
 * 
 * </pre>
 * 
 * @see https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#info-object
 *
 */
public class InfoObject extends MetaObject {
	/**
	 * The Terms of Service for the API.
	 */
	public String			termsOfService;

	/**
	 * The contact information for the exposed API.
	 */
	public ContactObject	contact;

	/**
	 * The license information for the exposed API.
	 */
	public LicenseObject	license;

	/**
	 * Required Provides the version of the application API (not to be
	 * confused with the specification version).
	 */
	@Required
	public String			version;
}

