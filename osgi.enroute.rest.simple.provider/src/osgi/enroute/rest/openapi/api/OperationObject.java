package osgi.enroute.rest.openapi.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.dto.DTO;

import osgi.enroute.rest.openapi.annotations.Required;

/**
 * Describes a single API operation on a path.
 * 
 * @see https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#operation-object
 */
public class OperationObject extends DTO {
	/**
	 * A list of tags for API documentation control. Tags can be used for
	 * logical grouping of operations by resources or any other qualifier.
	 */
	public String[]						tags;

	/**
	 * A short summary of what the operation does. For maximum readability in
	 * the swagger-ui, this field SHOULD be less than 120 characters.
	 */
	public String						summary;

	/**
	 * A verbose explanation of the operation behavior. GFM syntax can be used
	 * for rich text representation.
	 */
	public String						description;

	/**
	 * Additional external documentation for this operation.
	 */
	public ExternalDocumentationObject	externalDocs;

	/**
	 * Unique string used to identify the operation. The id MUST be unique among
	 * all operations described in the API. Tools and libraries MAY use the
	 * operationId to uniquely identify an operation, therefore, it is
	 * recommended to follow common programming naming conventions.
	 */
	public String						operationId;

	/**
	 * A list of MIME types the operation can consume. This overrides the
	 * consumes definition at the Swagger Object. An empty value MAY be used to
	 * clear the global definition. Value MUST be as described under Mime Types.
	 */
	public Set<String>					consumes	= new HashSet<>();

	/**
	 * A list of MIME types the operation can produce. This overrides the
	 * produces definition at the Swagger Object. An empty value MAY be used to
	 * clear the global definition. Value MUST be as described under Mime Types.
	 */
	public Set<String>					produces	= new HashSet<>();

	/**
	 * A list of parameters that are applicable for this operation. If a
	 * parameter is already defined at the Path Item, the new definition will
	 * override it, but can never remove it. The list MUST NOT include
	 * duplicated parameters. A unique parameter is defined by a combination of
	 * a name and location. The list can use the Reference Object to link to
	 * parameters that are defined at the Swagger Object's parameters. There can
	 * be one "body" parameter at most.
	 */
	public List<ParameterObject>		parameters	= new ArrayList<>();

	/**
	 * The request body applicable for this operation.
	 */

	public RequestBodyObject			requestBody;
	/**
	 * Required. The list of possible responses as they are returned from
	 * executing this operation.
	 */
	@Required
	public Map<String, ResponseObject>	responses	= new HashMap<>();

	/**
	 * The transfer protocol for the operation. Values MUST be from the list:
	 * "http", "https", "ws", "wss". The value overrides the Swagger Object
	 * schemes definition.
	 */
	public List<String>					schemes		= new ArrayList<>();

	/**
	 * Declares this operation to be deprecated. Usage of the declared operation
	 * should be refrained. Default value is false.
	 */
	public boolean						deprecated;

	/**
	 * A declaration of which security schemes are applied for this operation.
	 * The list of values describes alternative security schemes that can be
	 * used (that is, there is a logical OR between the security requirements).
	 * This definition overrides any declared top-level security. To remove a
	 * top-level security declaration, an empty array can be used.
	 */
	public Map<String, List<String>>	security;

	/**
	 * The host (name or ip) serving the path. This optional value will override
	 * the top-level host or path item-level host. if present.
	 */
	public HostObject					host;
}
