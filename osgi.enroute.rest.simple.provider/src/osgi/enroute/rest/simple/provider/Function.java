package osgi.enroute.rest.simple.provider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.lib.converter.Converter;
import aQute.lib.hex.Hex;
import osgi.enroute.rest.api.RESTRequest;

/**
 * Cache for information about the discovered methods to speed up the mapping
 * process. Includes CORS configuration as well, if available.
 */
class Function {
	final Method						method;
	final Object						target;
	final String						name;
	final int							cardinality;
	final boolean						varargs;
	final Type							post;
	final boolean						hasRequestParameter;
	final boolean						hasPayloadAsParameter;
	final java.lang.reflect.Parameter	parameters[];
	Method								bodyMethod;
	final Verb							verb;
	final String						path;
	final int							ranking;
	Map<String, String>					args		= Collections.emptyMap();
	final CORSUtil.CORSConfig           cors;

	static Converter					converter	= new Converter();
	static EnumSet<Verb>				PAYLOAD		= EnumSet.of(Verb.post,
			Verb.put);

	static {
		converter.hook(byte[].class, new Converter.Hook() {

			@Override
			public Object convert(Type dest, Object o) throws Exception {
				if (o instanceof String)
					return Hex.toByteArray((String) o);

				return null;
			}
		});
	}

	Function(Object target, Method method, Verb verb, String path,
			int ranking) {
		this.target = target;
		this.method = method;
		this.verb = verb;
		this.path = path;
		this.ranking = ranking;
		int cardinality = method.getParameterTypes().length;

		// check if the first parameter is of type RestRequest
		// and whether it has a _body() method defining the post Type

		Type requestBody = null;
		boolean hasRequesParam = false;
		if (cardinality > 0)
			try {
				Class<?> rc = method.getParameterTypes()[0];
				if (RESTRequest.class.isAssignableFrom(rc)) {
					@SuppressWarnings("unchecked")
					Class<? extends RESTRequest> rrc = (Class<? extends RESTRequest>) rc;
					hasRequesParam = true;
					cardinality--; // don't count as cardinality
					
					//
					// Calculate any decoded names for args
					//
					
					Map<String, String> args = new HashMap<>();
					RestMapper.getPublicMethod(rrc, RESTRequest.class)
							.filter(m -> !m.getName().startsWith("_"))
							.forEach(m -> {
								String decoded = RestMapper.decode(m.getName(), false);
								if (!m.getName().equals(decoded))
									args.put(decoded, m.getName());
							});

					if (!args.isEmpty())
						this.args = args;

					//
					// This might silently fail if there is
					// no such method
					//
					
					this.bodyMethod = rc.getMethod("_body");
					requestBody = bodyMethod.getGenericReturnType();
				}

			} catch (Exception e) {
				// Ignore
			}
		this.hasRequestParameter = hasRequesParam;

		// if method starts with put/post, and no _body method defined,
		// then convert payload data to first method parameter after
		// RestRequest in this case this parameter does not count for
		// the cardinality

		if (requestBody == null && (PAYLOAD.contains(verb))) {
			if (cardinality == 0)
				throw new IllegalArgumentException("Invalid " + verb
						+ " method " + method.getName() + ". A payload method "
						+ PAYLOAD
						+ " must have a RESTRequest subclass with a _body method defined or a parameter that acts as the body type.");
			requestBody = method.getParameterTypes()[hasRequestParameter ? 1
					: 0];
			cardinality--; // don't count as cardinality
			hasPayloadAsParameter = true;
		} else {
			hasPayloadAsParameter = false;
		}

		if (path.equals("/") && cardinality > 0)
			throw new IllegalArgumentException("Invalid " + verb
					+ " method " + method.getName() + ". A method on the root path cannot have a non-zero cardinality.");

		this.post = requestBody;

		this.cardinality = cardinality;

		if ((varargs = method.isVarArgs()))
			this.name = verb + path.substring(1);
		else
			this.name = verb + path.substring(1) + "/" + this.cardinality;

		int offset = 0;
		if (hasPayloadAsParameter)
			offset++;

		if (hasRequestParameter)
			offset++;

		int len = method.getParameters().length - offset;
		this.parameters = new java.lang.reflect.Parameter[len];
		System.arraycopy(method.getParameters(), offset, this.parameters, 0,
				len);

		cors = CORSUtil.parseCORS(target, method, name, verb, cardinality);
	}

	Type getPayloadType() {
		return post;
	}

	String getName() {
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
	public Object[] match(Map<String, Object> args, List<String> list)
			throws Exception {
		Object[] parameters = new Object[cardinality
				+ (hasRequestParameter ? 1 : 0)
				+ (hasPayloadAsParameter ? 1 : 0)];
		Type[] types = method.getGenericParameterTypes();
		if (hasRequestParameter) {
			parameters[0] = converter.convert(types[0], args);
		}

		for (int i = (hasRequestParameter ? 1 : 0)
				+ (hasPayloadAsParameter ? 1 : 0); i < parameters.length; i++) {
			if (varargs && i == parameters.length - 1) {
				parameters[i] = converter.convert(types[i], list);
			} else {
				// varargs but not enough to fill the constants
				if (list.isEmpty())
					return null;

				String removed = list.remove(0);
				parameters[i] = converter.convert(types[i], removed);
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
	public Object invoke(Object[] parameters) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		return method.invoke(target, parameters);
	}

	public java.lang.reflect.Parameter[] getParameters() {
		return parameters;
	}

	Verb getVerb() {
		return verb;
	}

	String getPath() {
		return path;
	}

	CORSUtil.CORSConfig getCORS() {
	    return cors;
	}
}
