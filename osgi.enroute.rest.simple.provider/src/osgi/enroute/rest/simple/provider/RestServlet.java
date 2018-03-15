package osgi.enroute.rest.simple.provider;

import java.io.Closeable;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import aQute.lib.json.JSONCodec;
import osgi.enroute.rest.api.REST;

/**
 * The REST servlet is responsible for mapping incoming requests to methods on
 * Resource Manager services (i.e. those registered services that implement
 * {@code REST}). Each servlet has its own endpoint and therefore its own
 * mapper.
 * 
 */
class RestServlet extends HttpServlet implements REST, Closeable {
	@Override
	public String toString() {
		return "RestServlet [servletPattern=" + servletPattern + ", closed="
				+ closed + "]";
	}

	private static final long	serialVersionUID	= 1L;
	final static JSONCodec		codec				= new JSONCodec();
	String						servletPattern;
	Config						config;
	boolean						angular;
	Closeable					closeable;
	static Random				random				= new Random();
	AtomicBoolean				closed				= new AtomicBoolean(false);
	final RestMapper			mapper;

	RestServlet(Config config, String namespace) {
		this.config = config;
		this.mapper = new RestMapper(namespace);
	}

	void setCloseable(Closeable c) {
		this.closeable = c;
	}

	public void service(HttpServletRequest rq, HttpServletResponse rsp) throws IOException, ServletException {
        if (config.requireSSL() && !isSecure(rq)) {
            rsp.sendError(config.notSecureError());
            return;
        }

        mapper.execute(rq, rsp);
	}

	public void close() throws IOException {
		if (closed.getAndSet(true))
			return;

		closeable.close();
	}

	synchronized void add(REST resource, int ranking) {
		mapper.addResource(resource, ranking);
	}

	synchronized void remove(REST resource) {
		mapper.removeResource(resource);
	}

	synchronized int count() {
	    return mapper.endpoints.size();
	}

	private static boolean isSecure(HttpServletRequest hreq) {
        if( hreq.isSecure() )
            return true;

        // If behind a proxy, check the "X-Forwarded-Proto" value set by the proxy server
        final String schemeHeader = hreq.getHeader( "X-Forwarded-Proto" );
        return "https".equals(schemeHeader);
    }
}
