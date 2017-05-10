package osgi.enroute.rest.openapi.api;

import java.net.URI;

import org.osgi.dto.DTO;

/**
 * 
 * Allows referencing an external resource for extended documentation.
 * 
 * 
 * https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#external-documentation-object
 */
public class ExternalDocumentationObject extends DTO {
	/**
	 * A short description of the target documentation. GFM syntax can be
	 * used for rich text representation.
	 */
	public String	description;
	/**
	 * Required. The URL for the target documentation. Value MUST be in the
	 * format of a URL.
	 */
	public URI		url;

}

