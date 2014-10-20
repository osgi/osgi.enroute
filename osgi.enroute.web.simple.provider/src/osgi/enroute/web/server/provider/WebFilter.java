package osgi.enroute.web.server.provider;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;

/**
 * Create a filter that limits the # of connections
 */
public class WebFilter implements Filter {

	int					maxConnections;
	String				maxConnectionMessage;
	AtomicInteger		counter = new AtomicInteger(1000);
	AtomicInteger		active	= new AtomicInteger();
	private Coordinator	coordinator;

	public WebFilter(int maxConnections, String maxConnectionMessage, Coordinator coordinator) {
		this.maxConnections = maxConnections;
		this.maxConnectionMessage = maxConnectionMessage;
		this.coordinator = coordinator;
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain chain) throws IOException,
			ServletException {

		try {
			HttpServletResponse hrsp = (HttpServletResponse) rsp;
			if (maxConnections > 0) {

				if (active.getAndIncrement() > maxConnections) {
					String msg = maxConnectionMessage;
					if (msg == null)
						msg = "Too many simultaneous requests (this is a small server, after all, this is not a commercial service)";
					hrsp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
					return;
				}
			}

			Coordination c = coordinator.begin("osgi.enroute.webrequest."+active.incrementAndGet(), 0);
			try {
				chain.doFilter(req, rsp);
				c.end();
			}
			catch (Throwable t) {
				c.fail(t);
				throw t;
			}
		}
		finally {
			active.decrementAndGet();
		}
	}

	//
	// These types of methods are superfluous in OSGI
	//
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

	@Override
	public void destroy() {
	}

}
