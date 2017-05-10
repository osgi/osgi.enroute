package osgi.enroute.rest.openapi.api;

import org.osgi.dto.DTO;

/**
 * An object representing a Host.
 *
 */
public class HostObject extends DTO {

	/**
	 * The host (name or ip) serving the API. This MUST be the host only and
	 * does not include the scheme nor sub-paths. It MAY include a port. If the
	 * host is not included, the host serving the documentation is to be used
	 * (including the port). The host does not support path templating.
	 */
	public String	host;

	/**
	 * The base path on which the API is served, which is relative to the host.
	 * If it is not included, the API is served directly under the host. The
	 * value MUST start with a leading slash (/). The basePath does not support
	 * path templating.
	 */
	public String	basePath;

	/**
	 * The transfer protocol of the API. Values MUST be from the list: "http",
	 * "https", "ws", "wss". If the scheme is not included, the default scheme
	 * to be used is the one used to access the OpenAPI definition itself.
	 */

	public TransferProtocol	scheme;
}
