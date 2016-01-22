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
									"name=" + TestServletB.NAME, 
					                Constants.SERVICE_RANKING + ":Integer=30",
									"no.index=true"
								},
		name 					= TestServletB.NAME, 
		configurationPolicy 	= ConfigurationPolicy.OPTIONAL)
public class TestServletB extends HttpServlet implements ConditionalServlet {

    static final String         NAME                = "osgi.enroute.simple.server.test.B";
    private static final long   serialVersionUID    = 1L;
    @Override

    public boolean doConditionalService( HttpServletRequest rq, HttpServletResponse rsp ) throws Exception {
        // Return false to test that C is the winner
        System.err.println("Trying ServletB");
        return false;
    }
}
