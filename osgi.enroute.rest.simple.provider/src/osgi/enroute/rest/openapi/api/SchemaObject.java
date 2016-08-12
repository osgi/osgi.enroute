package osgi.enroute.rest.openapi.api;

import osgi.enroute.rest.jsonschema.api.ContainerSchema;

/**
 * The Schema Object allows the definition of input and output data types. These
 * types can be objects, but also primitives and arrays. This object is based on
 * the JSON Schema Specification Draft 4 and uses a predefined subset of it. On
 * top of this subset, there are extensions provided by this specification to
 * allow for more complete documentation.
 * 
 * Further information about the properties can be found in JSON Schema Core and
 * JSON Schema Validation. Unless stated otherwise, the property definitions
 * follow the JSON Schema specification as referenced here.
 * 
 * The following properties are taken directly from the JSON Schema definition
 * and follow the same specifications:
 * 
 * $ref - As a JSON Reference format (See Data Type Formats for further details)
 * title description (GFM syntax can be used for rich text representation)
 * default (Unlike JSON Schema, the value MUST conform to the defined type for
 * the Schema Object) multipleOf maximum exclusiveMaximum minimum
 * exclusiveMinimum maxLength minLength pattern maxItems minItems uniqueItems
 * maxProperties minProperties required enum type The following properties are
 * taken from the JSON Schema definition but their definitions were adjusted to
 * the Swagger Specification. Their definition is the same as the one from JSON
 * Schema, only where the original definition references the JSON Schema
 * definition, the Schema Object definition is used instead.
 * 
 * items allOf properties additionalProperties Other than the JSON Schema subset
 * fields, the following fields may be used for further schema documentation.
 * 
 * 
 */
public class SchemaObject extends ContainerSchema {
	/**
	 * Adds support for polymorphism. The discriminator is the schema property
	 * name that is used to differentiate between other schema that inherit this
	 * schema. The property name used MUST be defined at this schema and it MUST
	 * be in the required property list. When used, the value MUST be the name
	 * of this schema or any schema that inherits it.
	 */
	public String						discriminator;
	/**
	 * Relevant only for Schema "properties" definitions. Declares the property
	 * as "read only". This means that it MAY be sent as part of a response but
	 * MUST NOT be sent as part of the request. Properties marked as readOnly
	 * being true SHOULD NOT be in the required list of the defined schema.
	 * Default value is false.
	 */
	public boolean						readOnly;
	/**
	 * This MAY be used only on properties schemas. It has no effect on root
	 * schemas. Adds Additional metadata to describe the XML representation
	 * format of this property.
	 */
	public XMLObject					xml					= null;

	/**
	 * Additional external documentation for this schema.
	 */
	public ExternalDocumentationObject	externalDocs;

	/**
	 * A free-form property to include an example of an instance for this
	 * schema.
	 */
	public Object						example;
	
	public boolean deprecated;
}