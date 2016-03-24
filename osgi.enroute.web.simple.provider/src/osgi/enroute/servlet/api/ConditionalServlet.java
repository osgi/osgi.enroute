package osgi.enroute.servlet.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The Servlet API in OSGi allows servlets to register URI's to which the
 * servlet should react. However, the servlet dispatcher makes deterministic
 * choice based on the registrations. This works well in most cases because
 * there is a prefix or pattern for the targeted URI. However, it does not work
 * well for situations where a Servlet needs to look up the URI before it can
 * decide it can handle the URI or not. This service API allows a servlet to be
 * called conditionally. The controller servlet can then iterate over the different
 * Servlet services in {@code service.ranking} order until one of them returns
 * true.
 */
public interface ConditionalServlet {
	/**
	 * The called servlet can execute the given request if it recognizes the
	 * requested URI or it can return false when it could not recognize the URI.
	 * The called servlet must not call any state changing methods like calling
	 * the input or output stream/reader get methods.
	 * <p>
	 * If this method throws an exception it will be logged and removed from the
	 * list. The request is assumed to have returned false in that case.
	 * 
	 * @param rq
	 *            The servlet request
	 * @param rsp
	 *            The servlet response
	 * @return @{code true} if the request was handled, {@code false} otherwise.
	 * @throws Exception
	 *             If thrown, the request is called and the servlet is removed
	 *             from the list of to be called services until re-registered.
	 */
	boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception;
}
