package osgi.enroute.web.server.exceptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.log.LogService;

public class ExceptionHandler {

	private final boolean			addTrailingSlash;
	private final LogService 		log;

	public ExceptionHandler(boolean addTrailingSlash, LogService log) {
		this.addTrailingSlash = addTrailingSlash;
		this.log = log;
	}

	public void handle(HttpServletRequest rq, HttpServletResponse rsp, Exception exception) {
		try {
			try {
				throw exception;
			} catch(FolderException e) {
				if(addTrailingSlash) {
					String path = e.getPath();
					if(!path.startsWith("/"))
						path = "/" + path;
					if(!path.endsWith("/"))
						path = path + "/";
					rsp.setHeader("Location", path);					
					rsp.sendRedirect(path);
				} else {
					// This is the default we will use if we don't add the trailing slash.
					// However, it is possible to imagine other types of responses as well.
					rsp.sendError(HttpServletResponse.SC_NOT_FOUND);					
				}
			} catch (Redirect302Exception e) {
				rsp.setHeader("Location", e.getPath());
				rsp.sendRedirect(e.getPath());
			} catch (NotFound404Exception e ) {
				rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (InternalServer500Exception e) {
				log.log(LogService.LOG_ERROR, "Internal webserver error", e);
				rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception ee) {
			log.log(LogService.LOG_ERROR, "Second level or unknown internal webserver error", ee);
		}
	}
}
