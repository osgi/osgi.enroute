package osgi.enroute.web.simple.test;

import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.*;
import org.osgi.service.http.whiteboard.*;
import osgi.enroute.servlet.api.*;

@Component(
		service 				= { Servlet.class, ConditionalServlet.class }, 
		immediate 				= true, 
		property 				= {
									HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=" + "/", 
									"name=" + BlacklistServlet.NAME, 
					                Constants.SERVICE_RANKING + ":Integer=5",
									"no.index=true"
								},
		name 					= BlacklistServlet.NAME, 
		configurationPolicy 	= ConfigurationPolicy.OPTIONAL)
public class BlacklistServlet extends HttpServlet implements ConditionalServlet {

    static final String         NAME                = "osgi.enroute.simple.server.test.blacklist";
    private static final long   serialVersionUID    = 1L;
    @Override

    public boolean doConditionalService( HttpServletRequest rq, HttpServletResponse rsp ) throws Exception {
        // Throw an exception to test whether or not this gets blacklisted
        System.err.println("Blacklisted");
        throw new Exception("I am a black sheep.");
    }
}
