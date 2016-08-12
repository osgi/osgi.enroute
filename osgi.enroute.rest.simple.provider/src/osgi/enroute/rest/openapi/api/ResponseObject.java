package osgi.enroute.rest.openapi.api;

import java.util.HashMap;
import java.util.Map;

import org.osgi.dto.DTO;

import osgi.enroute.rest.openapi.annotations.Required;

/**
 * Describes a single response from an API Operation.
 * 
 * A container for the expected responses of an operation. The container maps a
 * HTTP response code to the expected response. It is not expected from the
 * documentation to necessarily cover all possible HTTP response codes, since
 * they may not be known in advance. However, it is expected from the
 * documentation to cover a successful operation response and any known errors.
 * 
 * The default can be used a default response object for all HTTP codes that are
 * not covered individually by the specification.
 * 
 * The Responses Object MUST contain at least one response code, and it SHOULD
 * be the response for a successful operation call. Describes a single response
 * from an API Operation.
 * 
 * @see https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#response-object
 */
public class ResponseObject extends DTO {
	/**
	 * Required. A short description of the response. GFM syntax can be used for
	 * rich text representation.
	 */
	@Required
	public String						description;
	/**
	 * A definition of the response structure. It can be a primitive, an array
	 * or an object. If this field does not exist, it means no content is
	 * returned as part of the response. As an extension to the Schema Object,
	 * its root type value may also be "file". This SHOULD be accompanied by a
	 * relevant produces mime-type.
	 */
	public SchemaObject					schema;

	/**
	 * A list of headers that are sent with the response.
	 */
	public Map<String, HeaderObject>	headers		= new HashMap<>();

	/**
	 * An example of the response message.
	 */
	public Map<String, Object>			examples	= new HashMap<>();

	/**
	 * An object representing operations related to the response payload.
	 * 
	 */
	//public Map<String, LinkObject>		links;
}
