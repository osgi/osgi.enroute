package osgi.enroute.rest.openapi.api;

import java.util.Map;

import org.osgi.dto.DTO;

public class RequestBodyObject extends DTO {
	/**
	 * A brief description of the request body. This could contain examples of use. GFM syntax can be used for rich text representation.
	 */
	public String description;
	/**
	 * The schema defining the type used for the request body.
	 */
	public SchemaObject schema;
	/**
	 * Examples of the request body, referenced by media type.
	 */
	public Map<String,Object> examples;
	
	/**
	 * Determines if the request body is required in the request. Defaults to true.
	 */
	public boolean required = true; 
}
