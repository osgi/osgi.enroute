package osgi.enroute.rest.simple.provider;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

import javax.servlet.http.*;

import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;
import aQute.lib.collections.*;
import aQute.lib.converter.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.json.*;

/**
 * Utility code to map a web request to a object and method request efficiently.
 * It uses the reflection information in the registered object's methods to
 * convert the parameters to an appropriate value.
 * <p>
 * A method is applicable if it starts with the web requests method
 * (GET/PUT/DELETE/OPTION/HEAD etc) and then in its lower case form. The
 * remaining name (with the first character made upper case) is the first
 * segment after this mapper's prefix. So /rest/name with PUT request is mapped
 * to methods with the name {@code putName}.
 * <p>
 * Applicable methods are methods that start with a an interface argument that
 * can be backed by a Map; this map will contain the web request's arguments,
 * i.e. the parameters after the question mark. The {@link Options} interface
 * has methods that provide access to the underlying servlet request and
 * response.
 * <p>
 * 
 * <pre>
 * interface MyOptions extends Options {
 * 	int[] value();
 * }
 * 
 * int getFoo(MyOptions options) {
 * 	int sum = 1;
 * 	for (int i : options.value())
 * 		sum *= sum;
 * 	return sum;
 * }
 * // is mapped from https://localhost:8080/rest?value=45
 * 
 * </pre>
 * 
 * Since rest is supposed to carry the arguments in the URL, the segments in the
 * URL after the method name are mapped to arguments, potentially varargs. For
 * example:
 * 
 * <pre>
 * interface MyOptions extends Options {}
 * 
 * Attr getFoo(MyOptions options, byte[] id, int attr) {
 * 	return getObject(id).getAttr(attr);
 * }
 * 
 * // is mapped from https://localhost:8080/rest/0348E767F0/23
 * 
 * </pre>
 * 
 * For POST and PUT requests, the Options interface provides access to the
 * underlying stream. Just declare a field _ on the Options interface and this
 * method will return the converted value from the JSON decoded input stream:
 * 
 * <pre>
 * interface MyOptions extends Options {
 * 	Person _();
 * }
 * 
 * int getFoo(MyOptions options) {
 * 	Person p = options._();
 * 	return p.age;
 * }
 * // is mapped from a POST/PUT https://localhost:8080/rest
 * 
 * </pre>
 */
public class RestMapper {
	final static JSONCodec		codec		= new JSONCodec();
	MultiMap<String,Function>	functions	= new MultiMap<String,Function>();
	private final String		prefix;
	boolean						diagnostics;
	Converter					converter	= new Converter();

	{
		converter.hook(byte[].class, new Converter.Hook() {

			@Override
			public Object convert(Type dest, Object o) throws Exception {
				if (o instanceof String)
					return Hex.toByteArray((String) o);

				return null;
			}
		});
	}

	/**
	 * Cache for information about the discovered methods to speed up the
	 * mapping process.
	 */
	class Function {
		final Method	method;
		final Object	target;
		final String	name;
		final int		cardinality;
		final boolean	varargs;
		final Type		post;

		public Function(Object target, Method m) {
			this.target = target;
			this.method = m;
			this.cardinality = m.getParameterTypes().length;

			if ((varargs = m.isVarArgs()))
				this.name = m.getName().toLowerCase();
			else
				this.name = m.getName().toLowerCase() + "/" + cardinality;

			Type post = null;
			try {
				Class<?> rc = m.getParameterTypes()[0];
				post = rc.getMethod("_body").getGenericReturnType();
			}
			catch (Exception e) {
				// Ignore
			}
			this.post = post;
		}

		public Type getPost() {
			return post;
		}

		public String getName() {
			return name;
		}

		public String toString() {
			return name;
		}

		/**
		 * This is the heart, it tries to map the parameters to this method.
		 * 
		 * @param args
		 *            mapped to the Options interface (first parameter)
		 * @param list
		 *            the parameters of the call (segments in the url)
		 * @return the converted arguments or null if no possible match
		 */
		public Object[] match(Map<String,Object> args, ExtList<String> list) throws Exception {
			Object[] parameters = new Object[cardinality];
			Type[] types = method.getGenericParameterTypes();
			parameters[0] = converter.convert(types[0], args);

			for (int i = 1; i < cardinality; i++) {
				if (varargs && i == cardinality - 1) {
					parameters[i] = converter.convert(types[i], list);
				} else {
					// varargs but not enough to fill the constants
					if (list.isEmpty())
						return null;

					parameters[i] = converter.convert(types[i], list.remove(0));
				}
			}
			return parameters;
		}

		/**
		 * Invoke the method
		 * 
		 * @param parameters
		 *            the Options interface and optional segments
		 * @return the result of the invocation
		 */
		public Object invoke(Object[] parameters) throws IllegalArgumentException, IllegalAccessException,
				InvocationTargetException {
			return method.invoke(target, parameters);
		}
	}

	/**
	 * Create a mapper on the prefix. Will only react on requests that start
	 * with the given prefix.The remainder after the prefix is mapped to a
	 * method.
	 * 
	 * @param prefix
	 *            the prefix to match
	 */
	public RestMapper(String prefix) {
		this.prefix = prefix;

	}

	/**
	 * Add a new resource manager. Add all public methods that have the first
	 * argument be an interface that extends {@link Options}.
	 */
	public synchronized void addResource(REST resource) {
		for (Method m : resource.getClass().getMethods()) {
			if (m.getParameterTypes().length == 0)
				continue;

			Class< ? > argumentType = m.getParameterTypes()[0];
			if (!RESTRequest.class.isAssignableFrom(argumentType))
				continue;

			Function f = new Function(resource, m);
			functions.add(f.getName(), f);
		}
	}

	/**
	 * Remove a prior registered resource
	 */
	public synchronized void removeResource(REST resource) {
		for (String s : functions.keySet()) {
			List<Function> fs = functions.get(s);
			Iterator<Function> i = fs.iterator();
			while (i.hasNext()) {
				Function f = i.next();
				if (f.target == resource)
					i.remove();
			}
		}
	}

	/**
	 * Execute a web request.
	 * 
	 * @param rq
	 *            the request
	 * @param rsp
	 *            the response
	 * @return true if we matched and executed
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public boolean execute(HttpServletRequest rq, HttpServletResponse rsp) throws IOException {
		String path = rq.getPathInfo();
		if (path == null)
			throw new IllegalArgumentException(
					"The rest servlet requires that the name of the resource follows the servlet path ('rest'), like /rest/aQute.service.library.Program[/...]*[?...]");

		//
		// Check if this request is for us
		//
		if (prefix != null) {
			if (!path.startsWith(prefix))
				return false;

			path = path.substring(prefix.length());
		} else if (path.startsWith("/"))
			path = path.substring(1);

		//
		// Find the method's arguments embedded in the url
		//
		String[] segments = path.split("/");
		ExtList<String> list = new ExtList<String>(segments);
		String name = (rq.getMethod() + list.remove(0)).toLowerCase();
		int cardinality = list.size() + 1;

		//
		// We register methods with their cardinality to not have
		// to wade through them one by one
		//
		List<Function> candidates = functions.get(name + "/" + cardinality);
		if (candidates == null)
			candidates = functions.get(name);

		//
		// check if we found a suitable candidate
		//

		if (candidates == null)
			throw new FileNotFoundException("No such method " + name + "/" + cardinality + ". Available: " + functions);

		//
		// All values are arrays, turn them into singletons when
		// they have one element
		//
		Map<String,Object> args = new HashMap<String,Object>(rq.getParameterMap());
		for (Map.Entry<String,Object> e : args.entrySet()) {
			Object o = e.getValue();
			if (o.getClass().isArray()) {
				if (Array.getLength(o) == 1)
					e.setValue(Array.get(o, 0));
			}
		}

		//
		// Provide the context variables through the Options
		// interface
		//
		args.put("_request", rq);
		args.put("_host", rq.getHeader("Host"));
		args.put("_response", rsp);
		try {
			//
			// Find the functions matching the
			// name

			for (Function f : candidates) {
				Object[] parameters = f.match(args, list);
				if (parameters != null) {

					//
					// Handle the POST or PUT
					// request. If so, convert the
					// requests input stream via JSON
					// to the object with the type of the
					// Options _ method.
					//
					Type type = f.getPost();
					if (type != null
							&& (rq.getMethod().equalsIgnoreCase("POST") || rq.getMethod().equalsIgnoreCase("PUT"))) {
						Object arguments = codec.dec().from(rq.getInputStream()).get(type);
						args.put("_body", arguments);
					}

					//
					// Invoke the method, throw the underlying exception if any
					//

					Object result;
					try {
						result = f.invoke(parameters);
					}
					catch (InvocationTargetException e1) {
						throw e1.getTargetException();
					}

					//
					// Check if we can compress the result
					//
					OutputStream out = rsp.getOutputStream();

					if (result != null) {
						//
						// < 14 bytes screws up
						//
						if (!(result instanceof Number) && !(result instanceof String && ((String) result).length() < 100)) {
							String acceptEncoding = rq.getHeader("Accept-Encoding");
							if (acceptEncoding != null) {
								boolean gzip = acceptEncoding.indexOf("gzip") >= 0;
								boolean deflate = acceptEncoding.indexOf("deflate") >= 0;

								if (gzip) {
									out = new GZIPOutputStream(out);
									rsp.setHeader("Content-Encoding", "gzip");
								} else if (deflate) {
									out = new DeflaterOutputStream(out);
									rsp.setHeader("Content-Encoding", "deflate");
								}
							}
						}

						//
						// Convert based on the returned object
						// Streams, byte[], File, and CharSequence
						// are written without conversion. Other objects
						// are written with json
						//

						if (result instanceof InputStream) {
							IO.copy((InputStream) result, out);
						} else if (result instanceof byte[]) {
							byte[] data = (byte[]) result;
							rsp.setContentLength(data.length);
							out.write(data);
						} else if (result instanceof File) {
							File fresult = (File) result;
							rsp.setContentLength((int) fresult.length());
							IO.copy(fresult, out);
						} else {
							rsp.setContentType("application/json;charset=UTF-8");
							if (result instanceof Iterable)
								result = new ExtList<Object>((Iterable<Object>) result);
							codec.enc().writeDefaults().to(out).put(result);
						}
					}
					out.close();
				}
			}
			return false;
		}
		catch (FileNotFoundException e) {
			rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		catch (SecurityException e) {
			rsp.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
		catch (UnsupportedOperationException e) {
			rsp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		catch (IllegalStateException e) {
			rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
		catch (Throwable e) {
			e.printStackTrace();
			rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return true;
	}

	public void setDiagnostics(boolean on) {
		this.diagnostics = true;
	}
}
