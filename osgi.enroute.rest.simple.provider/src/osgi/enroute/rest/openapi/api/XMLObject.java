package osgi.enroute.rest.openapi.api;

import org.osgi.dto.DTO;

/**
 * A metadata object that allows for more fine-tuned XML model definitions.
 * 
 * When using arrays, XML element names are not inferred (for
 * singular/plural forms) and the name property should be used to add that
 * information. See examples for expected behavior.
 * 
 * @see https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#xml-object
 */
public class XMLObject extends DTO {
	/**
	 * Replaces the name of the element/attribute used for the described
	 * schema property. When defined within the Items Object (items), it
	 * will affect the name of the individual XML elements within the list.
	 * When defined alongside type being array (outside the items), it will
	 * affect the wrapping element and only if wrapped is true. If wrapped
	 * is false, it will be ignored.
	 */
	public String	name;
	/**
	 * The URL of the namespace definition. Value SHOULD be in the form of a
	 * URL.
	 */
	public String	namespace;

	/**
	 * The prefix to be used for the name.
	 */
	public String	prefix;
	/**
	 * Declares whether the property definition translates to an attribute
	 * instead of an element. Default value is false.
	 */
	public boolean	attribute	= false;

	/**
	 * MAY be used only for an array definition. Signifies whether the array
	 * is wrapped (for example, <books><book/><book/></books>) or unwrapped
	 * (<book/><book/>). Default value is false. The definition takes effect
	 * only when defined alongside type being array (outside the items).
	 */
	public boolean	wrapped;

}


