package osgi.enroute.rest.jsonschema.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class ContainerSchema extends PrimitiveSchema {

	/**
	 * Both of these keywords can be used to decorate a user interface with
	 * information about the data produced by this user interface. A title will
	 * preferrably be short, whereas a description will provide explanation
	 * about the purpose of the instance described by this schema.
	 * 
	 * Both of these keywords MAY be used in root schemas, and in any
	 * subschemas.
	 * 
	 */
	public String						title;
	public String						description;

	/**
	 * Items Object Required if type is "array". Describes the type of items in
	 * the array.
	 * 
	 * The value of "items" MUST be either an object or an array. If it is an
	 * object, this object MUST be a valid JSON Schema. If it is an array, items
	 * of this array MUST be objects, and each of these objects MUST be a valid
	 * JSON Schema.
	 * 
	 * Successful validation of an array instance with regards to these two
	 * keywords is determined as follows:
	 * 
	 * if "items" is not present, or its value is an object, validation of the
	 * instance always succeeds, regardless of the value of "additionalItems";
	 * 
	 * if the value of "additionalItems" is boolean value true or an object,
	 * validation of the instance always succeeds;
	 * 
	 * if the value of "additionalItems" is boolean value false and the value of
	 * "items" is an array, the instance is valid if its size is less than, or
	 * equal to, the size of "items".
	 */
	public List<ContainerSchema>		items				= new ArrayList<>();

	/**
	 * An array instance is valid against "maxItems" if its size is less than,
	 * or equal to, the value of this keyword.
	 * 
	 * 
	 */
	public int							maxItems			= Integer.MAX_VALUE;
	/**
	 * An array instance is valid against "minItems" if its size is greater
	 * than, or equal to, the value of this keyword.
	 */
	public int							minItems			= 0;
	/**
	 * 
	 * If this keyword has boolean value false, the instance validates
	 * successfully. If it has boolean value true, the instance validates
	 * successfully if all of its elements are unique.
	 * 
	 * 
	 */
	public boolean						uniqueItems;

	/**
	 * The value of "additionalItems" MUST be either a boolean or an object. If
	 * it is an object, this object MUST be a valid JSON Schema.
	 */
	public Object						additionalItems;

	/**
	 * The value of "properties" MUST be an object. Each value of this object
	 * MUST be an object, and each object MUST be a valid JSON Schema.
	 * 
	 * Successful validation of an object instance against these three keywords
	 * depends on the value of "additionalProperties":
	 * 
	 * if its value is boolean true or a schema, validation succeeds;
	 * 
	 * if its value is boolean false, the algorithm to determine validation
	 * success is described below.
	 */
	public Map<String, ContainerSchema>	properties			= new HashMap<>();

	/**
	 * The value of "patternProperties" MUST be an object. Each property name of
	 * this object SHOULD be a valid regular expression, according to the ECMA
	 * 262 regular expression dialect. Each property value of this object MUST
	 * be an object, and each object MUST be a valid JSON Schema.
	 */
	public Map<String, ContainerSchema>	patternProperties;

	/**
	 * The value of "additionalProperties" MUST be a boolean or an object. If it
	 * is an object, it MUST also be a valid JSON Schema.
	 * 
	 * In this case, validation of the instance depends on the property set of
	 * "properties" and "patternProperties". In this section, the property names
	 * of "patternProperties" will be called regexes for convenience.
	 * 
	 * The first step is to collect the following sets:
	 * 
	 * s The property set of the instance to validate. p The property set from
	 * "properties". pp The property set from "patternProperties". Having
	 * collected these three sets, the process is as follows:
	 * 
	 * remove from "s" all elements of "p", if any;
	 * 
	 * for each regex in "pp", remove all elements of "s" which this regex
	 * matches.
	 * 
	 * Validation of the instance succeeds if, after these two steps, set "s" is
	 * empty.
	 */
	public Object					additionalProperties;

	/**
	 * dependencies
	 * 
	 * 
	 * TOC 5.4.5.1. Valid values
	 * 
	 * This keyword's value MUST be an object. Each value of this object MUST be
	 * either an object or an array.
	 * 
	 * If the value is an object, it MUST be a valid JSON Schema. This is called
	 * a schema dependency.
	 * 
	 * If the value is an array, it MUST have at least one element. Each element
	 * MUST be a string, and elements in the array MUST be unique. This is
	 * called a property dependency.
	 * 
	 * 
	 * TOC 5.4.5.2. Conditions for successful validation
	 * 
	 * 
	 */
	// dependencies

	/**
	 * An object instance is valid against "maxProperties" if its number of
	 * properties is less than, or equal to, the value of this keyword.
	 * 
	 */
	public int							maxProperties		= Integer.MAX_VALUE;
	/**
	 * An object instance is valid against "minProperties" if its number of
	 * properties is greater than, or equal to, the value of this keyword.
	 * 
	 * 
	 */
	public int							minProperties;
	/**
	 * The value of this keyword MUST be an array. This array MUST have at least
	 * one element. Elements of this array MUST be strings, and MUST be unique.
	 * 
	 * An object instance is valid against this keyword if its property set
	 * contains all elements in this keyword's array value.
	 * 
	 */
	public String[]						required;

	/**
	 * An instance validates successfully against this keyword if it validates
	 * successfully against all schemas defined by this keyword's value.
	 */

	public List<ContainerSchema>		allOf;

	/**
	 * An instance validates successfully against this keyword if it validates
	 * successfully against at least one schema defined by this keyword's value.
	 * 
	 * 
	 */
	public List<ContainerSchema>		anyOf;

	/**
	 * An instance validates successfully against this keyword if it validates
	 * successfully against exactly one schema defined by this keyword's value.
	 */
	public List<ContainerSchema>		oneOf;

	/**
	 * An instance is valid against this keyword if it fails to validate
	 * successfully against the schema defined by this keyword.
	 */
	public ContainerSchema				not;
}