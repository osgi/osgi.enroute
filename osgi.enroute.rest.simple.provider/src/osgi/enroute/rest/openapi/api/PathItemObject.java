package osgi.enroute.rest.openapi.api;

import java.util.ArrayList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * Describes the operations available on a single path. A Path Item may be
 * empty, due to ACL constraints. The path itself is still exposed to the
 * documentation viewer but they will not know which operations and parameters
 * are available.
 * 
 * 
 * <pre>
 * {
"get": {
"description": "Returns pets based on ID",
"summary": "Find pets by ID",
"operationId": "getPetsById",
"produces": [
  "application/json",
  "text/html"
],
"responses": {
  "200": {
    "description": "pet response",
    "schema": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/Pet"
      }
    }
  },
  "default": {
    "description": "error payload",
    "schema": {
      "$ref": "#/definitions/ErrorModel"
    }
  }
}
},
"parameters": [
{
  "name": "id",
  "in": "path",
  "description": "ID of pet to use",
  "required": true,
  "type": "array",
  "items": {
    "type": "string"
  },
  "collectionFormat": "csv"
}
]
}
 * </pre>
 * 
 * @see https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#path-item-object
 *
 */
public class PathItemObject extends DTO {

	/**
	 * An optional, string summary, intended to apply to all operations in this
	 * path.
	 */
	public String			summary;

	/**
	 * An optional, string description, intended to apply to all operations in
	 * this path.
	 */
	public String			description;

	/**
	 * Allows for an external definition of this path item. The referenced
	 * structure MUST be in the format of a Path Item Object. If there are
	 * conflicts between the referenced definition and this Path Item's
	 * definition, the behavior is undefined.
	 */
	public String			$ref;
	/**
	 * A definition of a GET operation on this path.
	 */
	public OperationObject	get;
	/**
	 * A definition of a PUT operation on this path.
	 */
	public OperationObject	put;
	/**
	 * A definition of a POST operation on this path.
	 */
	public OperationObject	post;
	/**
	 * A definition of a DELETE operation on this path.
	 */
	public OperationObject	delete;
	/**
	 * A definition of a OPTIONS operation on this path.
	 */
	public OperationObject	options;
	/**
	 * A definition of a HEAD operation on this path.
	 */
	public OperationObject	head;
	/**
	 * A definition of a PATCH operation on this path.
	 */
	public OperationObject	patch;

	/**
	 * A list of parameters that are applicable for all the operations described
	 * under this path. These parameters can be overridden at the operation
	 * level, but cannot be removed there. The list MUST NOT include duplicated
	 * parameters. A unique parameter is defined by a combination of a name and
	 * location. The list can use the Reference Object to link to parameters
	 * that are defined at the Swagger Object's parameters. There can be one
	 * "body" parameter at most.
	 */
	public List<ParameterObject>		parameters	= new ArrayList<>();
	
	/**
	 * The host serving the path. This optional value will override the top-level host if present. 
	 */
	public HostObject host;

}
