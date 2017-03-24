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
	boolean						corsEnabled;
	Closeable					closeable;
	static Random				random				= new Random();
	AtomicBoolean				closed				= new AtomicBoolean(false);
	final RestMapper			mapper;

	RestServlet(Config config, String namespace) {
		this.config = config;
		corsEnabled = config.corsEnabled();
		this.mapper = new RestMapper(namespace);
	}

	void setCloseable(Closeable c) {
		this.closeable = c;
	}

	public void service(HttpServletRequest rq, HttpServletResponse rsp) throws IOException, ServletException {
		if (corsEnabled) {
			addCorsHeaders(rsp);
		}

		if ("OPTIONS".equalsIgnoreCase(rq.getMethod())) {
			doOptions(rq, rsp);
		} else {
			mapper.execute(rq, rsp);
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
		rsp.setHeader("Access-Control-Allow-Methods", config.allowedMethods());
		rsp.setHeader("Access-Control-Allow-Headers", config.allowHeaders());
		rsp.addIntHeader("Access-Control-Max-Age", config.maxAge());
		rsp.setHeader("Allow", config.allowedMethods());
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
}
