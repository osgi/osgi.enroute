package osgi.enroute.rest.openapi.api;

import osgi.enroute.rest.jsonschema.api.PrimitiveSchema;

public class ItemsObject extends PrimitiveSchema  {
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
	public ItemsObject	items;

	/**
	 * An array instance is valid against "maxItems" if its size is less than,
	 * or equal to, the value of this keyword.
	 * 
	 * 
	 */
	public int			maxItems;
	/**
	 * An array instance is valid against "minItems" if its size is greater
	 * than, or equal to, the value of this keyword.
	 */
	public int			minItems			= 0;
	/**
	 * 
	 * If this keyword has boolean value false, the instance validates
	 * successfully. If it has boolean value true, the instance validates
	 * successfully if all of its elements are unique.
	 * 
	 * 
	 */
	public boolean		uniqueItems;
	/**
	 * string Determines the format of the array if type array is used. Possible
	 * values are: csv - comma separated values foo,bar. ssv - space separated
	 * values foo bar. tsv - tab separated values foo\tbar. pipes - pipe
	 * separated values foo|bar. Default value is csv.
	 */
	public String		collectionFormat;
}
