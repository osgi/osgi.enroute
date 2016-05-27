package osgi.enroute.web.server.provider;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.component.annotations.*;
import org.osgi.service.http.whiteboard.*;
import org.osgi.service.log.*;

import osgi.enroute.web.server.cache.*;

@Component(
		name = "osgi.enroute.web.service.provider.bfs",
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/bnd", 
				"name=DispatchServlet", 
				Constants.SERVICE_RANKING + ":Integer=100"
		}, 
		service = Servlet.class, 
		configurationPolicy = ConfigurationPolicy.OPTIONAL, 
		immediate = true)
public class BundleFileServer extends HttpServlet {

	private static final long					serialVersionUID	= 1L;

	WebServerConfig								config;

	@Reference LogService						log;
	@Reference Cache							cache;

	@Activate
	void activate(WebServerConfig config) throws Exception {
		this.config = config;
	}

	public void service(HttpServletRequest rq, HttpServletResponse rsp) throws ServletException, IOException {
		String path = rq.getRequestURI();
		if (path != null && path.startsWith("/"))
			path = path.substring(1);

		Bundle b = null;
		if (b == null) {
			rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		try {
			FileCache c = cache.getFromBundle(b, path);
		}
		catch (Exception e) {
			log.log(LogService.LOG_ERROR, "Internal webserver error", e);
			if (config.exceptions())
				throw new RuntimeException(e);

			try {
				PrintWriter pw = rsp.getWriter();
				pw.println("Internal server error\n");
				rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			catch (Exception ee) {
				log.log(LogService.LOG_ERROR, "Second level internal webserver error", ee);
			}
		}
	}
}