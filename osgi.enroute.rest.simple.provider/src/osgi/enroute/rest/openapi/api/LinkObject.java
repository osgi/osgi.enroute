package osgi.enroute.rest.openapi.api;

import java.net.URI;

import org.osgi.dto.DTO;

public class LinkObject extends DTO {
	/**
	 * a relative or absolute URL to a linked resource. This field is mutually
	 * exclusive with the operationId field.
	 */
	public URI		href;

	/**
	 * the name of an existing, resolvable OAS operation, as defined with a
	 * unique operationId. This field is mutually exclusive with the href field.
	 * Relative href values may be used to locate an existing Operation Object
	 * in the OAS.
	 */
	public String	operationId;
	
	/**
	 * an Object representing parameters to pass to an operation as specified with operationId or identified via href.
	 */
	
	//public Map<String,LinkParametersObject> parameters;
	
	/**
	 * an Object representing headers to pass to the linked resource.
	 */
	
	//public Map<String,LinkHeaderObject> headers;
	
	/**
	 * a description of the link, supports GFM.
	 */
	public String description;
}
