package osgi.enroute.rest.openapi.api;

import java.net.URI;

import org.osgi.dto.DTO;

/**
 * Contact information for the exposed API.
 * 
 * <pre>
 * { 
 * 	"name": "API Support", 
 * 	"url": "http://www.swagger.io/support", 
 * 	"email": "support@swagger.io" 
 * }
 * </pre>
 * 
 * @see https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#contact-object
 *
 */

public class ContactObject extends DTO {
	/**
	 * The identifying name of the contact person/organization.
	 */
	public String	name;

	/**
	 * The URL pointing to the contact information. MUST be in the format of
	 * a URL.
	 */
	public URI		url;

	/**
	 * The email address of the contact person/organization. MUST be in the
	 * format of an email address.
	 */
	public String	email;

}

