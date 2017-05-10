package osgi.enroute.rest.jsonschema.api;

import org.osgi.dto.DTO;

public class PrimitiveSchema extends DTO {
	/**
	 * string Required. The type of the object. The value MUST be one of
	 * {@link PrimitiveType}
	 * 
	 * The value of this keyword MUST be either a string or an array. If it is
	 * an array, elements of the array MUST be strings and MUST be unique.
	 * 
	 * String values MUST be one of the seven primitive types defined by the
	 * core specification.
	 * 
	 * 
	 * An instance matches successfully if its primitive type is one of the
	 * types defined by keyword. Recall: "number" includes "integer".
	 * 
	 * 
	 */
	public PrimitiveType			type;
	/**
	 * string The extending format for the previously mentioned type. See Data
	 * Type Formats for further details.
	 */
	public String		format;
	/**
	 * Declares the value of the header that the server will use if none is
	 * provided. (Note: "default" has no meaning for required headers.) See
	 * http://json-schema.org/latest/json-schema-validation.html#anchor101.
	 * Unlike JSON Schema this value MUST conform to the defined type for the
	 * header.
	 * 
	 * This keyword can be used to supply a default JSON value associated with a
	 * particular schema. It is RECOMMENDED that a default value be valid
	 * against the associated schema.
	 * 
	 * This keyword MAY be used in root schemas, and in any subschemas.
	 */
	public Object		default_;
	/**
	 * Successful validation depends on the presence and value of
	 * "exclusiveMaximum":
	 * 
	 * if "exclusiveMaximum" is not present, or has boolean value false, then
	 * the instance is valid if it is lower than, or equal to, the value of
	 * "maximum";
	 * 
	 * if "exclusiveMaximum" has boolean value true, the instance is valid if it
	 * is strictly lower than the value of "maximum".
	 */
	public double		maximum;
	/**
	 * If "exclusiveMaximum" is present, "maximum" MUST also be present.
	 * 
	 * @see #maximum
	 */
	public boolean		exclusiveMaximum	= false;
	/**
	 * Successful validation depends on the presence and value of
	 * "exclusiveMinimum":
	 * 
	 * if "exclusiveMinimum" is not present, or has boolean value false, then
	 * the instance is valid if it is greater than, or equal to, the value of
	 * "minimum";
	 * 
	 * if "exclusiveMinimum" is present and has boolean value true, the instance
	 * is valid if it is strictly greater than the value of "minimum".
	 * 
	 * 
	 */
	public double		minimum = Double.MIN_VALUE;
	/**
	 * If "exclusiveMinimum" is present, "minimum" MUST also be present.
	 * 
	 * 
	 * {@see #minimum}
	 */
	public boolean		exclusiveMinimum;

	/**
	 * A string instance is valid against this keyword if its length is less
	 * than, or equal to, the value of this keyword.
	 * 
	 * The length of a string instance is defined as the number of its
	 * characters as defined by RFC 4627 [RFC4627].
	 */
	public int			maxLength;
	/**
	 * A string instance is valid against this keyword if its length is greater
	 * than, or equal to, the value of this keyword.
	 * 
	 * The length of a string instance is defined as the number of its
	 * characters as defined by RFC 4627 [RFC4627].
	 */
	public int			minLength			= 0;
	/**
	 * A string instance is considered valid if the regular expression matches
	 * the instance successfully. Recall: regular expressions are not implicitly
	 * anchored.
	 */
	public String		pattern;
	/**
	 * An instance validates successfully against this keyword if its value is
	 * equal to one of the elements in this keyword's array value.
	 */
	public Object[]	enum_;
	/**
	 * A numeric instance is valid against "multipleOf" if the result of the
	 * division of the instance by this keyword's value is an integer.
	 */
	public double		multipleOf;

}
