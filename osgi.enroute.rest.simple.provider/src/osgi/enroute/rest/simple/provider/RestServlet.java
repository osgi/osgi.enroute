package osgi.enroute.rest.simple.provider;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.lib.converter.Converter;
import aQute.lib.json.JSONCodec;
import osgi.enroute.http.capabilities.RequireHttpImplementation;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RestConstants;

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
@RequireHttpImplementation
@ProvideCapability(ns=ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name=RestConstants.REST_SPECIFICATION_NAME, version=RestConstants.REST_SPECIFICATION_VERSION)
@Designate(ocd = RestServlet.Config.class)
@Component(
	//
	service = Servlet.class, //
	name = "osgi.enroute.rest.simple", //
	configurationPolicy = ConfigurationPolicy.OPTIONAL, //
	property=HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN+"=/rest/*"
)
public class RestServlet extends HttpServlet implements REST {
	private static final long	serialVersionUID	= 1L;
	final static JSONCodec		codec				= new JSONCodec();
	RestMapper					mapper				= new RestMapper(null);

	public RestServlet() {
		addREST(this);
	}
	@ObjectClassDefinition
	@interface Config {
		boolean angular();
		boolean corsEnabled() default false;

		String osgi_http_whiteboard_servlet_pattern();
		
		//CORS header Access-Control-Allow-Origin
		String allowOrigin() default "*";
		//CORS header Access-Control-Allow-Methods
		String allowMethods() default "GET, POST, PUT";
		//CORS header Access-Control-Allow-Headers
		String allowHeaders() default "Content-Type";
		//CORS Access-Control-Max-Age
		int  maxAge() default 86400;
		//CORS Allow methods
		String allowedMethods() default "GET, HEAD, POST, TRACE, OPTIONS";
	}

	Config	config;
	boolean	angular;
	boolean corsEnabled;
	
	@Activate
	void actvate(Map<String, Object> map) throws Exception {
		config = Converter.cnv(Config.class, map);

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
		
		if(corsEnabled){
			addCorsHeaders(rsp);		
		}
		
		if("OPTIONS".equalsIgnoreCase(rq.getMethod())){
			try {
				doOptions(rq, rsp);
			} catch (ServletException e) {
				throw new IOException(e);
			}
		}
		mapper.execute(rq, rsp);
	}

	/*
	 * this is required to handle the Client requests with Request METHOD
	 * &quot;OPTIONS&quot;
	 */
	protected void doOptions(HttpServletRequest rq, HttpServletResponse rsp) throws ServletException, IOException {
		
		super.doOptions(rq, rsp);
	}
	
	@Reference( cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	synchronized void addREST(REST resourceManager) {
		mapper.addResource(resourceManager);
	}

	synchronized void removeREST(REST resourceManager) {
		mapper.removeResource(resourceManager);
	}
	

	private void addCorsHeaders(HttpServletResponse rsp) {
		rsp.setHeader("Access-Control-Allow-Origin", config.allowOrigin());
		rsp.setHeader("Access-Control-Allow-Methods", config.allowMethods());
		rsp.setHeader("Access-Control-Allow-Headers", config.allowHeaders());
		rsp.addIntHeader("Access-Control-Max-Age", config.maxAge());
		rsp.setHeader("Allow", config.allowedMethods());		
	}

}
