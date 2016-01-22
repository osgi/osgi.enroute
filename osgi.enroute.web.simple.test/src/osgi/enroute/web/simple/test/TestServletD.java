package osgi.enroute.web.simple.test;

import java.io.PrintWriter;

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
									"name=" + TestServletD.NAME, 
					                Constants.SERVICE_RANKING + ":Integer=10",
									"no.index=true"
								},
		name 					= TestServletD.NAME, 
		configurationPolicy 	= ConfigurationPolicy.OPTIONAL)
public class TestServletD extends HttpServlet implements ConditionalServlet {

    static final String         NAME                = "osgi.enroute.simple.server.test.D";
    private static final long   serialVersionUID    = 1L;
    @Override

    public boolean doConditionalService( HttpServletRequest rq, HttpServletResponse rsp ) throws Exception {
        // This should never get called, because C should win
        PrintWriter writer = rsp.getWriter();
        System.err.println("Trying ServletD");
        writer.println("You should not see this. If you do, something went wrong.");
        return true;
    }
}
