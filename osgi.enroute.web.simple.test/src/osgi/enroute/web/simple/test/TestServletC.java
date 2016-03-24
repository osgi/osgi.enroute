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
									"name=" + TestServletC.NAME, 
					                Constants.SERVICE_RANKING + ":Integer=20",
									"no.index=true"
								},
		name 					= TestServletC.NAME, 
		configurationPolicy 	= ConfigurationPolicy.OPTIONAL)
public class TestServletC extends HttpServlet implements ConditionalServlet {

    static final String         NAME                = "osgi.enroute.simple.server.test.C";
    private static final long   serialVersionUID    = 1L;
    @Override

    public boolean doConditionalService( HttpServletRequest rq, HttpServletResponse rsp ) throws Exception {
        PrintWriter writer = rsp.getWriter();
        System.err.println("Trying ServletC");
        writer.println("This is the winner!");
        return true;
    }
}
