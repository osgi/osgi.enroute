package osgi.enroute.rest.openapi.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

import osgi.enroute.rest.openapi.annotations.Required;

/**
 * Swagger Object
 * 
 * This is the root document object for the API specification.It combines what
 * previously was the Resource Listing and API Declaration(version 1.2 and
 * earlier)together into one document.
 * 
 * @see https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#swagger-object
 */
public class OpenAPIObject extends DTO {

	/**
	 * Required. Specifies the Swagger Specification version being used. It can
	 * be used by the Swagger UI and other clients to interpret the API listing.
	 * The value MUST be "2.0".
	 */
	@Required
	public String							openapi			= "3.0.0";

	/**
	 * Required. Provides metadata about the API. The metadata can be used by
	 * the clients if needed.
	 */
	@Required
	public InfoObject						info;

	/**
	 * An array of Host objects which provide scheme, host, port, and basePath
	 * in an associative manner.
	 */
	public List<HostObject>					hosts			= new ArrayList<>();

	/**
	 * A list of MIME types the APIs can consume. This is global to all APIs but
	 * can be overridden on specific API calls. Value MUST be as described under
	 * Mime Types.
	 */
	public List<String>						consumes;

	/**
	 * A list of MIME types the APIs can produce. This is global to all APIs but
	 * can be overridden on specific API calls. Value MUST be as described under
	 * Mime Types.
	 */
	public List<String>						produces;

	/**
	 * An object to hold responses that can be used across operations. This
	 * property does not define global responses for all operations.
	 */
	public Map<String, ResponseObject>		responses		= new HashMap<>();;

	/**
	 * Required. The available paths and operations for the API.
	 */
	@Required
	public Map<String, PathItemObject>		paths			= new HashMap<>();

	/**
	 * An element to hold various schemas for the specification.
	 */
	public Map<String, Object>				components		= new HashMap<>();

	/**
	 * A declaration of which security schemes are applied for the API as a
	 * whole. The list of values describes alternative security schemes that can
	 * be used (that is, there is a logical OR between the security
	 * requirements). Individual operations can override this definition.
	 */
	public List<Map<String, List<String>>>	security;

	/**
	 * A list of tags used by the specification with additional metadata. The
	 * order of the tags can be used to reflect on their order by the parsing
	 * tools. Not all tags that are used by the Operation Object must be
	 * declared. The tags that are not declared may be organized randomly or
	 * based on the tools' logic. Each tag name in the list MUST be unique.
	 */
	public List<TagObject>					tags;

	/**
	 * Additional external documentation.
	 */
	public ExternalDocumentationObject		externalDocs;
}
