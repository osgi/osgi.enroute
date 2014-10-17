package osgi.enroute.web.server.provider;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class ExceptionFilter implements Filter {


	@Override
	public void doFilter(ServletRequest rq, ServletResponse rsp, FilterChain chain) throws IOException,
			ServletException {
		try {
			chain.doFilter(rq, rsp);
		} catch (Throwable t) {
			try {
				rsp.setContentType("text/plain");
				PrintWriter writer = rsp.getWriter();

				writer.println("<h1>Server Error</h1>");
				writer.println("<pre>");
				t.printStackTrace(writer);
				writer.println("</pre>");

			} catch (Exception e) {
				// could not write the exception
				
			}
			if ( !rsp.isCommitted())
				((HttpServletResponse) rsp).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
}
