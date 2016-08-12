package osgi.enroute.rest.openapi.api;

import org.osgi.dto.DTO;

import osgi.enroute.rest.openapi.annotations.Required;

public class MetaObject extends DTO {
	/**
	 * Required. The title of the application.
	 */
	@Required
	public String			title;

	/**
	 * A short description of the application. GFM syntax can be used for
	 * rich text representation.
	 */
	public String			description;


}
