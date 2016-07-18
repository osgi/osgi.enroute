package osgi.enroute.rest.simple.provider;

import java.io.IOException;
import java.util.Random;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import aQute.lib.json.JSONCodec;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.UriMapper;

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
@Designate(ocd = Config.class)
@Component(
	service = Servlet.class,
	name = "osgi.enroute.rest.simple.servlet",
	configurationPolicy = ConfigurationPolicy.REQUIRE,
	immediate = true
)
public class RestServlet extends HttpServlet implements REST {
	private static final long	serialVersionUID	= 1L;
	final static JSONCodec		codec				= new JSONCodec();
	String                      servletPattern;

	Config	config;
	boolean	angular;
	boolean corsEnabled;

	@Reference RestController controller;

	@Activate
	void actvate(Config config) throws Exception {
		corsEnabled = config.corsEnabled();
		servletPattern = config.osgi_http_whiteboard_servlet_pattern();
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
		}else{
		    // Go through the UriMappers to determine the correct namespace
		    for(UriMapper uriMapper : controller.uriMappersFor(servletPattern)) {
		        String namespace = uriMapper.namespaceFor(rq.getRequestURI());
		        if(namespace != null ) {
	                RestMapper restMapper = controller.restMapperFor(namespace);
	                if(restMapper != null ) {                   
	                    restMapper.execute(rq, rsp);
	                    return;
	                }
		        }
		    }
		    // Should always fall through to the default mapper... if it does not, there is an Internal Error
            throw new IllegalStateException("Could not find Mapper");
		}
	}

    /*
	 * this is required to handle the Client requests with Request METHOD
	 * &quot;OPTIONS&quot; typically the preflight requests
	 */
	protected void doOptions(HttpServletRequest rq, HttpServletResponse rsp) throws ServletException, IOException {
		super.doOptions(rq, rsp);
	}

	private void addCorsHeaders(HttpServletResponse rsp) {
		rsp.setHeader("Access-Control-Allow-Origin", config.allowOrigin());
		rsp.setHeader("Access-Control-Allow-Methods", config.allowMethods());
		rsp.setHeader("Access-Control-Allow-Headers", config.allowHeaders());
		rsp.addIntHeader("Access-Control-Max-Age", config.maxAge());
		rsp.setHeader("Allow", config.allowedMethods());		
	}
}
