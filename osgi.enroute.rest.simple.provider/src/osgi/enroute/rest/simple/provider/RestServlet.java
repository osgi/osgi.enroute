package osgi.enroute.rest.simple.provider;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import osgi.enroute.rest.api.REST;
import aQute.bnd.annotation.component.*;
import aQute.lib.converter.*;
import aQute.lib.json.*;

/**
 * The rest servlet is responsible for mapping incoming requests to methods on
 * Resource Manager services. Methods on these services are name
 * {@code <verb><segment[0]>} (Where the segment's first character is upper
 * cases.) For example /rest/user is mapped to {@code getUser}. If the methods'
 * first argument extends the Options interface then such an object is created
 * and backed by the request's parameters. Any subsequent segments are mapped to
 * the arguments of the methods. Varargs can be used to get all of them and any
 * segment is converted to the proper types if possible.
 * <p/>
 * 
 */

@Component(
	//
	provide = Servlet.class, //
	name = "osgi.enroute.rest.simple", //
	designate = RestServlet.Config.class, //
	configurationPolicy = ConfigurationPolicy.optional, //
	properties="alias=/rest"
)
public class RestServlet extends HttpServlet {
	private static final long	serialVersionUID	= 1L;
	final static JSONCodec		codec				= new JSONCodec();
	RestMapper					mapper				= new RestMapper(null);

	interface Config {
		boolean angular();

		String alias();
	}

	Config	config;
	boolean	angular;

	@Activate
	void actvate(Map<String, Object> map) throws Exception {
		config = Converter.cnv(Config.class, map);
		String alias = config.alias();
		if (alias == null)
			alias = "/rest";

		// TODO log if ends with /
		angular = config.angular();
	}

	static Random	random	= new Random();

	public void service(HttpServletRequest rq, HttpServletResponse rsp) throws IOException {
		String pathInfo = rq.getPathInfo();
		if (pathInfo == null) {
			rsp.getWriter()
					.println(
							"The rest servlet requires that the name of the resource follows the servlet path ('rest'), like /rest/aQute.service.library.Program[/...]*[?...]");
			rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		mapper.execute(rq, rsp);
	}

	@Reference(
		type = '*')
	synchronized void addREST(REST resourceManager) {
		mapper.addResource(resourceManager);
	}

	synchronized void removeREST(REST resourceManager) {
		mapper.removeResource(resourceManager);
	}
}
