package osgi.enroute.rest.simple.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.collections.ExtList;
import aQute.lib.collections.MultiMap;
import aQute.lib.converter.Converter;
import aQute.lib.getopt.Options;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.json.Decoder;
import aQute.lib.json.Encoder;
import aQute.lib.json.JSONCodec;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTResponse;

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
 * interface MyOptions extends RESTRequest {
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
 * interface MyOptions extends RESTRequest {
 * }
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
 * underlying stream. Just declare a field _body on the Options interface and this
 * method will return the converted value from the JSON decoded input stream:
 * 
 * <pre>
 * interface MyOptions extends RESTRequest {
 * 	Person _body();
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
	static Logger			log				= LoggerFactory
			.getLogger(RestMapper.class);
	// TODO: seems to be a duplication of ACCEPTED_METHOD_NAMES_P
//	final static Pattern	METHOD_NAME_P	= Pattern
//			.compile("(?<verb>get|post|put|delete|option|head)(?<path>.*)");
	final List<REST>		endpoints		= new CopyOnWriteArrayList<>();
	final static JSONCodec	codec			= new JSONCodec();
	final static JSONCodec	codecNoNull		= new JSONCodec();
	static {
		codecNoNull.setIgnorenull(true);
	}
	Map<String, FunctionGroup>                             functionGroups   = new HashMap<String, FunctionGroup>(); 
	MultiMap<String, Function>								functions		= new MultiMap<String, Function>();
	boolean													diagnostics;

	/**
	 * Add a new resource manager. Add all public methods that have the first
	 * argument be an interface that extends {@link Options}.
	 */
	final static Pattern	ACCEPTED_METHOD_NAMES_P	= Pattern
			.compile("(?<verb>get|post|put|delete|option|head)(?<path>.*)");
	final String			namespace;

	public RestMapper(String namespace) {
		this.namespace = namespace;
	}

	public synchronized void addResource(REST resource, int ranking) {
		for (Method m : resource.getClass().getMethods()) {
			if (m.getDeclaringClass() == Object.class)
				continue;

			if (Modifier.isStatic(m.getModifiers()))
				continue;

			// restrict to methods starting with HTTP request prefix
			String methodName = m.getName();
			Matcher matcher = ACCEPTED_METHOD_NAMES_P.matcher(methodName);
			if (!matcher.lookingAt())
				continue;

			Verb verb = Verb.valueOf(Verb.class, matcher.group("verb"));
			String path = "/" + decode(matcher.group("path"));

			Function function = new Function(resource, m, verb, path, ranking);
			String groupName = function.getName().substring(function.getVerb().name().length());
			if (!functionGroups.containsKey(groupName))
			    functionGroups.put(groupName, new FunctionGroup(groupName, null, null, null, null));
			functionGroups.put(groupName, new FunctionGroup(functionGroups.get(groupName), function, verb));
			functions.add(function.getName(), function);

			Collections.sort(functions.get(function.getName()),
					(a, b) -> Integer.compare(a.ranking, b.ranking));
		}

		endpoints.add(resource);
	}

	static String decode(String path) {
		return decode(path,true);
	}
	
	static String decode(String path, boolean toLower) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			switch (c) {
			case '_':
				sb.append('-');
				break;
			case '$':
				c = (char) (Hex.nibble(path.charAt(i + 1)) * 16
						+ Hex.nibble(path.charAt(i + 2)));
				i += 2;
				sb.append(c);
				break;
			default:
				if ( toLower )
					sb.append(Character.toLowerCase(c));
				else
					sb.append(c);
				break;
			}

		}
		//
		// Skip last _ so we can escape keywords
		//
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-')
			sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Remove a prior registered resource
	 */
	public synchronized void removeResource(REST resource) {
		endpoints.remove(resource);
		for (String s : functions.keySet()) {
			List<Function> fs = functions.get(s);
			Iterator<Function> i = fs.iterator();
			while (i.hasNext()) {
				Function f = i.next();
				if (f.target == resource) {
					i.remove();
					for (FunctionGroup fg : functionGroups.values()) {
					    Verb v = fg.hasFunction(f);
					    if (v != null) {
					        FunctionGroup fg2 = new FunctionGroup(fg, null, v);
					        functionGroups.put(fg.name, fg2);
					           if (!fg2.hasFunction())
					               functionGroups.remove(fg.name);
					    }
					}
				}
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
	public boolean execute(HttpServletRequest rq, HttpServletResponse rsp)
			throws IOException {
		try {
            String path = rq.getPathInfo();
			if (path == null)
				path = "";
			else if (path.startsWith("/"))
				path = path.substring(1);

			if (path.equals("openapi.json")) {
			    // TODO: is this true for all method types? Or should this be only for GET?
			    // TODO: how do we treat CORS for this special response?
				return doOpenAPI(rq, rsp);
			}

			//
			// Find the method's arguments embedded in the url
			//
			String[] segments = path.split("/");
			ExtList<String> list = new ExtList<String>(segments);
            String first = list.remove(0).toLowerCase();
            String actualMethod = rq.getMethod().toLowerCase();
            boolean isHead = "head".equals(actualMethod);
            String effectiveMethod = isHead ? "get" : actualMethod;
			String name = (effectiveMethod + first);
			int cardinality = list.size();
            String group = first + "/" + cardinality;

			//
			// We register methods with their cardinality to not have
			// to wade through them one by one
			//
			List<Function> candidates = functions.get(name + "/" + cardinality);
			if (candidates == null)
				candidates = functions.get(name);

            // TODO: Consider implementing strict adherence (i.e. case-sensitivity) checks to methods.
            //       (Methods are supposed to be all upper case.)
            boolean isOptions = "OPTIONS".equalsIgnoreCase(rq.getMethod());

            //
			// check if we found a suitable candidate
			//
            FunctionGroup fg = functionGroups.get(group);
            if ((candidates == null || candidates.isEmpty()) && !isOptions) {
			    if (fg != null) {
			        // The URI exists, but not with this method. Return a 405.
			        rsp.setHeader("Allow", fg.getOptions());
			        throw new NoSuchMethodException();			        
			    } else {
			        // Return a 404.
	                throw new FileNotFoundException("No such method " + name + "/"
	                        + cardinality + ". Available: " + functions);
			    }
			}

			//
			// All values are arrays, turn them into singletons when
			// they have one element
			//
			Map<String, Object> args = new HashMap<String, Object>(
					rq.getParameterMap());
			

			for (Map.Entry<String, Object> e : args.entrySet()) {
				Object o = e.getValue();
				if (o.getClass().isArray()) {
					if (Array.getLength(o) == 1)
						e.setValue(Array.get(o, 0));
				}
			}

			for (String header : Collections.list(rq.getHeaderNames())) {
				List<String> values = Collections.list(rq.getHeaders(header));
				header = header.toUpperCase();
				if (values.size() == 1)
					args.put(header, values.get(0));
				else
					args.put(header, values);
			}

			//
			// Provide the context variables through the Options interface
			//
			args.put("_request", rq);
			args.put("_host", rq.getHeader("Host"));
			args.put("_response", rsp);

			if (!isOptions && candidates.isEmpty())
				return false;

			// The function should be null if and only if it is an OPTIONS request
			Function f = isOptions ? null : candidates.get(0);

			// Do OPTIONS before CORS
            if (isOptions)
                doOptions(rq, rsp, group);

            // CORS needs to be processed for all method types, so call before exiting from OPTIONS request
            doCORS(rq, rsp, f, fg, isOptions);

            // Don't need to do any more processing for OPTIONS requests
            if (isOptions)
                return true;

			Object[] parameters = f.match(args, list);
			if (parameters != null) {

				if ( f.args != null) {
					//
					// We have to rename variables if we have 
					// method names on the rest request that
					// are decoded.
					//
					for ( Entry<String, String> e : f.args.entrySet()) {
						Object value = args.get(e.getKey());
						if ( value != null)
							args.put(e.getValue(), value);
					}
				}

				Type type = f.getPayloadType();
				if (type != null) {
					Object payload = null;
					Decoder d = codec.dec().from(rq.getInputStream());
					if (!d.isEof())
						payload = d.get(type);

					if (f.hasPayloadAsParameter) {
						parameters[f.hasRequestParameter ? 1 : 0] = payload;
					}

					if (payload != null)
						args.put("_body", payload);
				}

				try {
                    Object result = f.invoke(parameters);
                    if (result != null) {
                        if ( result instanceof RESTResponse)
                            doRestResponse(rq, rsp, (RESTResponse) result, isHead);
                        else
                            printOutput(rq, rsp, result, isHead);
                    }
				} catch (InvocationTargetException e1) {
					throw e1.getTargetException();
				}
			}

		} catch (RESTResponse e) {

			doRestResponse(rq, rsp, e, false);

		} catch (Throwable e) {
			int code = ResponseException.getStatusCode(e.getClass());
			e.printStackTrace();
			rsp.setStatus(code);
		}
		return true;
	}

	private void doRestResponse(HttpServletRequest rq, HttpServletResponse rsp, RESTResponse e, boolean isHead) {
		doResponseHeaders(rsp, e);

		if (e.getContentType() != null)
			rsp.setContentType(e.getContentType());
		else
			rsp.setContentType("application/json");

		rsp.setStatus(e.getStatusCode());

		if (e.getValue() != null)
			try {
				printOutput(rq, rsp, e.getValue(), isHead);
			} catch (Exception e1) {
				log.error("failed to marshall value", e1);
				rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
	}

    private void doOptions(HttpServletRequest rq, HttpServletResponse rsp, String groupName) throws FileNotFoundException {
        rsp.setStatus(204);
        FunctionGroup group = functionGroups.get(groupName);
        if (group == null)
            throw new FileNotFoundException();
        rsp.setHeader("Allow", group.getOptions());
    }

    private void doCORS(HttpServletRequest rq, HttpServletResponse rsp, Function f, FunctionGroup fg, boolean isOptions) {
        CORSUtil.CORSConfig cors = null;
        // If it is an OPTIONS request, things are a bit more complicated.
        // The configuration we actually use depends in part on the method requested.
        if (isOptions) {
            String requestedMethod = rq.getHeader("Access-Control-Request-Method");
            Verb v = null;
            try {
                v = Verb.valueOf(requestedMethod.toLowerCase());
                cors = fg.optionsConfig().apply(v);
            } catch (Exception e) {
                // Bad verb requested. Let the CORS request play out.
            }
            
        } else {
            cors = f.getCORS();
        }
        if (cors != null) {
            // If Access-Control-Allow-Origin is determined dynamically, set "Vary: Origin" as a hint to proxy servers
            if (cors.dynamicOrigin)
                rsp.addHeader( "Vary", "Origin");
            if (CORSUtil.isCORSRequest(rq))
                CORSUtil.doCORS(rq, rsp, cors, fg);
        } else if (CORSUtil.isCORSRequest(rq)) {
            // If this is CORS request, but there is no CORS configuration, return a 403
            // TODO: write to log
            throw new SecurityException();
        }
    }

    private boolean doOpenAPI(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
		OpenAPI openAPI = 
		new OpenAPI(this,
				new URI(rq.getRequestURL().toString()));
		printOutput(rq, rsp, openAPI, false);
		return true;
	}

	void doResponseHeaders(HttpServletResponse rsp, RESTResponse e) {
		try {

			getPublicFields(e.getClass(), RESTResponse.class) //
					.forEach(headerField -> {
						try {
							Object o = headerField.get(e);
							if (o != null) {
								String headerName = decode(
										headerField.getName()).toUpperCase();
								if (o instanceof Collection
										|| o.getClass().isArray()) {
									// Use csv? pipe? See collectionFormat
									String[] values = Converter
											.cnv(String[].class, o);
									for (String value : values)
										rsp.addHeader(headerName, value);
								} else
									rsp.addHeader(headerName, o.toString());
							}
						} catch (Exception ex) {
							log.error("Field to get header from field "
									+ headerField, ex);
						}
					});

		} catch (Exception ee) {
			log.error("Unexpected reflection error", ee);
		}
	}

	static <T> Stream<Field> getPublicFields(Class<? extends T> clazz,
			Class<T> base) {
		return Stream.of(clazz.getFields()) //
				.filter(f -> !Modifier.isStatic(f.getModifiers()))//
				.filter(f -> true);
	}

	static <T> Stream<Method> getPublicMethod(Class<? extends T> clazz,
			Class<T> base) {
		return Stream.of(clazz.getMethods()) //
				.filter(f -> !Modifier.isStatic(f.getModifiers()))//
				.filter(f -> true);
	}

	public void setDiagnostics(boolean on) {
		this.diagnostics = true;
	}

	private void printOutput(HttpServletRequest rq, HttpServletResponse rsp, Object result, boolean isHead) throws Exception {
		printOutput(rq, rsp, result, codec.enc(), isHead);
	}

	@SuppressWarnings("unchecked")
	private void printOutput(HttpServletRequest rq, HttpServletResponse rsp, Object result, Encoder enc, boolean isHead) throws Exception {
		//
		// Check if we can compress the result
		//
		OutputStream out = rsp.getOutputStream();

		if (result != null) {
			//
			// < 14 bytes screws up
			//
			if (!(result instanceof Number) && !(result instanceof String
					&& ((String) result).length() < 100)) {
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

			if (result instanceof InputStream && !isHead) {
				IO.copy((InputStream) result, out);
			} else if (result instanceof byte[]) {
				byte[] data = (byte[]) result;
				rsp.setContentLength(data.length);
                if (!isHead)
				    out.write(data);
			} else if (result instanceof File) {
				File fresult = (File) result;
				rsp.setContentLength((int) fresult.length());
                if (!isHead)
				    IO.copy(fresult, out);
			} else {
				rsp.setContentType("application/json;charset=UTF-8");
				if (result instanceof Iterable)
					result = new ExtList<Object>((Iterable<Object>) result);
                // TODO: Content-Length should be set properly (for both GET and HEAD)
				// TODO: Consider using the new OSGi Converter??
				if (!isHead)
				    enc.writeDefaults().to(out).put(result);
			}
		}
		out.close();
	}

	String reverseEncode(String s) {
		StringBuilder sb = new StringBuilder();
		sb.append( reverseEncode(s.charAt(0), Character::isJavaIdentifierStart));
		
		for ( int i=1; i<s.length(); i++) {
			sb.append( reverseEncode(s.charAt(i), Character::isJavaIdentifierPart));
		}
		return sb.toString();
	}

	private String reverseEncode(char c, Predicate<Character> predicate) {
		if (c == '-')
			return "_";
		if (c == '_')
			return "$5F";
		

		if ( predicate.test(c))
			return Character.toString(c);
		
		if ( c > 0x7F)
			return "~";
		
		return "$" + Hex.nibble( c / 16) + Hex.nibble(c%16);
	}
}
