package osgi.enroute.jsonrpc.simple.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Servlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import aQute.lib.converter.Converter;
import aQute.lib.hex.Hex;
import aQute.lib.json.JSONCodec;
import osgi.enroute.http.capabilities.RequireHttpImplementation;
import osgi.enroute.jsonrpc.api.JSONRPC;
import osgi.enroute.jsonrpc.dto.JSON.Endpoint;
import osgi.enroute.jsonrpc.dto.JSON.JSONRPCError;
import osgi.enroute.jsonrpc.dto.JSON.Request;
import osgi.enroute.jsonrpc.dto.JSON.Response;

/**
 * The jsonrpc servlet is responsible for mapping incoming requests to methods
 * on JSONRPC services.
 * <p/>
 */
@RequireHttpImplementation
@Designate(ocd = JSONRpcServlet.Config.class)
@Component(//
name = "osgi.web.jsonrpc", //
service = Servlet.class, //
configurationPolicy = ConfigurationPolicy.OPTIONAL, property = {
		HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=" + "/jsonrpc/2.0/*" })
public class JSONRpcServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	final static Converter converter = new Converter();
	final static JSONCodec codec = new JSONCodec();

	static {
		codec.setIgnorenull(true);
	}

	final ConcurrentHashMap<String, JSONRPC> endpoints = new ConcurrentHashMap<String, JSONRPC>();
	final AtomicInteger ping = new AtomicInteger(10000);

	static {
		converter.hook(byte[].class, new Converter.Hook() {

			@Override
			public Object convert(Type dest, Object o) throws Exception {
				if (o instanceof String) {
					String s = (String) o;
					if (Hex.isHex(s))
						return Hex.toByteArray(s);
				}
				return null;
			}
		});
	}
	
	@ObjectClassDefinition
	@interface Config {
		boolean angular();

		boolean trace();
		
		String osgi_http_whiteboard_servlet_pattern();
	}


	Config config;
	boolean angular;
	boolean trace = false;
	@Reference
	LogService log;

	@Activate
	void actvate(Config config) throws Exception {
		this.config = config;
		angular = config.angular();
		trace = config.trace();
	}

	static Random random = new Random();

	public void service(HttpServletRequest rq, HttpServletResponse rsp) throws IOException {

		if (angular && rq.getMethod().equalsIgnoreCase("GET")) {
			// Angular helps us fight the
			// http://en.wikipedia.org/wiki/Cross-site_request_forgery
			// attack. At first get we set a cookie with a nonce (a random
			// number). For the remaining
			// session we expect the javascript to copy the cookie's value into
			// the X-XSRF-TOKEN header.
			String nonce = (String) rq.getSession().getAttribute("XSRF-TOKEN");
			if (nonce == null) {
				nonce = random.nextDouble() + "";

				Cookie xsrf = new Cookie("XSRF-TOKEN", nonce);
				rsp.addCookie(xsrf);
			} else {
				String xsrftoken = rq.getHeader("X-XSRF-TOKEN");
				if (xsrftoken == null || !xsrftoken.equals(nonce))
					throw new SecurityException("Forbidden since no X-XSRF-TOKEN");
			}
		}

		String pathInfo = rq.getPathInfo();
		if (pathInfo == null) {
			rsp.getWriter().println("Missing endpoint name in " + rq.getRequestURI());
			rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		if (pathInfo.startsWith("/"))
			pathInfo = pathInfo.substring(1);
		if (pathInfo.endsWith("/"))
			pathInfo = pathInfo.substring(0, pathInfo.length() - 1);

		try {
			Request request = codec.dec().from(rq.getInputStream()).get(Request.class);
			try {

				if (trace)
					System.out.println("Request " + request);
				Object o;
				if (request.method.startsWith("__")) {
					// internal commands
					o = this;
					request.params.add(0, rsp);
					request.params.add(0, rq);
					request.params.add(0, request);
					request.params.add(0, pathInfo);
				} else {
					o = endpoints.get(pathInfo);

					if (o == null) {
						rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
						return;
					}
				}

				Response result = execute(request, o);
				if (trace)
					System.out.println("Result " + result);

				OutputStream out = rsp.getOutputStream();

				if (result != null) {
					rsp.setContentType("application/json;charset=UTF-8");
					codec.enc().writeDefaults().to(out).put(result);
				}

				out.close();
			} catch (Exception e) {
				rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
		} catch (Exception e) {
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	public Response execute(Request request, Object target) throws Exception {
		Response response = new Response();
		response.id = request.id;
		try {
			for (Method m : target.getClass().getMethods()) {
				if (m.getName().equals(request.method)) {
					Object[] parameters = coerce(m, new ArrayList<Object>(request.params));
					if (parameters != null) {
						response.result = m.invoke(target, parameters);
						return response;
					}
				}
			}
			response.error = new JSONRPCError();
			response.error.message = "No such method " + request.method;
			return response;
		} catch (InvocationTargetException e) {
			log.log(LogService.LOG_INFO, "JSONRPC target error on " + request.toString(), e.getTargetException());
			response.error = new JSONRPCError();
			response.error.message = e.getTargetException().getMessage();
			if (response.error.message == null)
				response.error.message = e.getTargetException().toString();
			return response;
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, "JSONRPC exec error on " + request.toString(), e);
			response.error = new JSONRPCError();
			response.error.message = e.getMessage();
			return response;
		}
	}

	private Object[] coerce(Method m, List<Object> params) throws Exception {
		Type[] tp = m.getGenericParameterTypes();
		for (int i = 0; i < tp.length; i++) {
			if (tp[i] instanceof GenericArrayType) {
				Type c = ((GenericArrayType) tp[i]).getGenericComponentType();
				if (c == byte.class)
					tp[i] = byte[].class; // will use hex conversion as set by
											// the converter
			}
		}

		Object[] parameters = new Object[tp.length];
		int i = 0;
		for (; i < tp.length; i++) {
			if (params.isEmpty()) {
				if (m.isVarArgs() && i == tp.length - 1) {
					parameters[i] = Converter.cnv(tp[i], new Object[0]);
					break;
				}
				return null;
			}

			if (i == tp.length - 1 && m.isVarArgs()) {
				// last arg, varargs
				parameters[i] = converter.convert(tp[i], params);
				params.clear();
			} else {
				// normal arg
				parameters[i] = converter.convert(tp[i], params.remove(0));
			}
		}
		if (params.isEmpty())
			return parameters;
		else
			return null;
	}

	/**
	 * Provide a list of all the methods supported by this endpoint
	 * 
	 * @param resourceManager
	 * @param map
	 */

	/**
	 * This method is called by the JS code to get a list of endpoints.
	 * 
	 * @param endpointName
	 * @param request
	 * @param rq
	 * @param rsp
	 * @return
	 * @throws Exception
	 */
	public Endpoint __hi(String endpointName, Request request, HttpServletRequest rq, HttpServletResponse rsp)
			throws Exception {

		JSONRPC endpointImpl = endpoints.get(endpointName);
		if (endpointImpl == null)
			return null;

		Endpoint endpointDescription = new Endpoint();
		endpointDescription.name = endpointName;
		endpointDescription.descriptor = endpointImpl.getDescriptor();

		// TODO make 1 time
		for (Method m : endpointImpl.getClass().getMethods()) {
			if (m.getDeclaringClass() != Object.class)
				endpointDescription.methods.add(m.getName());
		}
		return endpointDescription;
	}

	static public class PingResponse extends DTO {
		public String nonce;
		public long time;
		public int sequence;
	}

	public PingResponse __ping(String endpointName, Request request, HttpServletRequest rq, HttpServletResponse rsp,
			String nonce) {
		PingResponse pr = new PingResponse();
		pr.nonce = nonce;
		pr.time = System.currentTimeMillis();
		pr.sequence = ping.getAndIncrement();
		return pr;
	}

	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	public synchronized void addEndpoint(osgi.enroute.jsonrpc.api.JSONRPC resourceManager, Map<String, Object> map) {
		String name = (String) map.get(JSONRPC.ENDPOINT);
		endpoints.put(name, resourceManager);
	}

	public synchronized void removeEndpoint(JSONRPC resourceManager, Map<String, Object> map) {
		String name = (String) map.get(JSONRPC.ENDPOINT);
		endpoints.remove(name);
	}

}
