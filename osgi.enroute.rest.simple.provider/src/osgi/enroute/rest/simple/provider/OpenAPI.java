package osgi.enroute.rest.simple.provider;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.dto.DTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.collections.MultiMap;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;
import osgi.enroute.rest.api.RESTResponse;
import osgi.enroute.rest.jsonschema.api.PrimitiveSchema;
import osgi.enroute.rest.jsonschema.api.PrimitiveType;
import osgi.enroute.rest.openapi.annotations.Description;
import osgi.enroute.rest.openapi.annotations.Info;
import osgi.enroute.rest.openapi.annotations.Required;
import osgi.enroute.rest.openapi.annotations.Tags;
import osgi.enroute.rest.openapi.annotations.ValidatorArray;
import osgi.enroute.rest.openapi.annotations.ValidatorNumber;
import osgi.enroute.rest.openapi.annotations.ValidatorObject;
import osgi.enroute.rest.openapi.annotations.ValidatorString;
import osgi.enroute.rest.openapi.api.ContactObject;
import osgi.enroute.rest.openapi.api.HeaderObject;
import osgi.enroute.rest.openapi.api.HostObject;
import osgi.enroute.rest.openapi.api.In;
import osgi.enroute.rest.openapi.api.InfoObject;
import osgi.enroute.rest.openapi.api.LicenseObject;
import osgi.enroute.rest.openapi.api.OpenAPIObject;
import osgi.enroute.rest.openapi.api.OperationObject;
import osgi.enroute.rest.openapi.api.ParameterObject;
import osgi.enroute.rest.openapi.api.PathItemObject;
import osgi.enroute.rest.openapi.api.RequestBodyObject;
import osgi.enroute.rest.openapi.api.ResponseObject;
import osgi.enroute.rest.openapi.api.SchemaObject;
import osgi.enroute.rest.openapi.api.TransferProtocol;

/**
 * Parse the REST Mappers contents and generate an Open API object of it.
 */
public class OpenAPI extends OpenAPIObject {
	static Logger					log		= LoggerFactory
			.getLogger(OpenAPI.class);

	private static final String[]	DEFLT	= new String[0];

	/**
	 * We completely build the object in the constructor.
	 */
	public OpenAPI(RestMapper restMapper, URI requestURL) throws Exception {
		openapi = "3.0.0.draft";
		hosts.add(doHost(restMapper, requestURL));

		this.info = doInfo(restMapper);

		//
		// We need to combine all functions with the same path but
		// different verbs into one operation so we create
		// an index on this path
		//

		MultiMap<String, Function> allOperations = getInverted(restMapper);

		for (Map.Entry<String, List<Function>> entry : allOperations
				.entrySet()) {
			String path = entry.getKey();
			List<Function> functions = entry.getValue();

			PathItemObject pathItemObject = toPathItemObject(functions);
			paths.put(path, pathItemObject);
		}
	}

	/*
	 * Set the info
	 */

	private InfoObject doInfo(RestMapper restMapper) throws URISyntaxException {
		InfoObject info = new InfoObject();
		for (REST r : restMapper.endpoints) {
			Info annotation = r.getClass().getAnnotation(Info.class);
			if (annotation != null) {
				info.title = annotation.title();
				info.description = annotation.description();
				if (info.description.isEmpty())
					info.description = getDescription(r.getClass());
				info.termsOfService = careful(null, annotation.termsOfService(),
						"");
				info.version = careful(null, annotation.version(), "");
				if (isSet(annotation.licenseName(), annotation.licenseUrl())) {
					info.license = new LicenseObject();
					info.license.name = careful(null, annotation.licenseName(),
							"");
					info.license.url = toUri(annotation.licenseUrl());
				}
				if (isSet(annotation.contactName(), annotation.contactEmail(),
						annotation.contactUrl())) {
					info.contact = new ContactObject();
					info.contact.email = careful(null,
							annotation.contactEmail(), "");
					info.contact.name = careful(null, annotation.contactName(),
							"");
					info.contact.url = toUri(annotation.contactUrl());
				}
				return info;
			}
		}
		if (restMapper.endpoints.isEmpty()) {
			info.description = "No endpoints found";
			info.title = "<>";
		} else {
			REST rest = restMapper.endpoints.iterator().next();
			info.title = rest.toString();
			info.description = rest.getClass().getName();
		}
		return info;
	}

	private boolean isSet(Object... any) {
		for (Object o : any) {
			if (o != null && !(o instanceof String && ((String) o).isEmpty()))
				return true;
		}
		return false;
	}

	/*
	 * Set the host information
	 */
	private HostObject doHost(RestMapper restMapper, URI requestURL)
			throws MalformedURLException, UnknownHostException {
		HostObject ho = new HostObject();
		ho.basePath = restMapper.namespace.substring(0,
				restMapper.namespace.length());
		ho.scheme = getTransferProtocol(requestURL.getScheme());
		ho.host = requestURL.getHost();

		if (requestURL.getPort() != -1
				&& requestURL.getPort() != requestURL.toURL().getDefaultPort())
			ho.host += ":" + requestURL.getPort();

		if (ho.host == null)
			ho.host = InetAddress.getLocalHost().getHostName();

		return ho;
	}

	/*
	 * Create a path item object
	 */
	private PathItemObject toPathItemObject(Collection<Function> functions)
			throws Exception {
		PathItemObject pathItemObject = new PathItemObject();
		for (Function f : functions) {
			OperationObject operationObject = getOperationObject(f);
			switch (f.getVerb()) {
			case delete:
				pathItemObject.delete = operationObject;
				break;
			case get:
				pathItemObject.get = operationObject;
				break;
			case head:
				pathItemObject.head = operationObject;
				break;
			case option:
				break;
			case post:
				pathItemObject.post = operationObject;
				break;
			case put:
				pathItemObject.put = operationObject;
				break;
			}
		}
		return pathItemObject;
	}

	/*
	 * Convert a function to an operation object
	 */
	private OperationObject getOperationObject(Function function)
			throws Exception {
		OperationObject operationObject = new OperationObject();
		Method method = function.method;

		operationObject.deprecated = getDeprecated(method);
		operationObject.description = getDescription(method);
		operationObject.summary = method.getName();
		operationObject.tags = getTags(method);
		operationObject.operationId = function.getName();

		doResponses(operationObject, function);
		doRequestBody(function, operationObject);
		doPathParameters(function, operationObject);
		doQueryParameters(function, operationObject);

		return operationObject;
	}

	/*
	 * Created an inverted list path -> function. The path depends on parameters
	 * through templating so we have to calculate this first
	 */

	private MultiMap<String, Function> getInverted(RestMapper restMapper) {
		MultiMap<String, Function> inverted = new MultiMap<>();

		for (Map.Entry<String, List<Function>> entry : restMapper.functions
				.entrySet()) {
			for (Function f : entry.getValue()) {
				String path = calculateTemplatePath(f);
				inverted.add(path, f);
			}
		}
		return inverted;
	}

	private String calculateTemplatePath(Function f) {
		StringBuilder path = new StringBuilder(f.getPath());
		for (Parameter parameter : f.getParameters()) {
			path.append("/{").append(parameter.getName()).append("}");
		}
		return path.toString();
	}

	private String[] getTags(AnnotatedElement ae) {
		Tags tags = ae.getAnnotation(Tags.class);
		if (tags == null)
			return null;

		return tags.value();
	}

	private boolean getDeprecated(AnnotatedElement ae) {
		return ae.getAnnotation(Deprecated.class) != null;
	}

	private boolean getRequired(AnnotatedElement ae) {
		return ae.getAnnotation(Required.class) != null;
	}

	private String getDescription(AnnotatedElement ae) {
		StringBuilder sb = new StringBuilder();

		Description annotation = ae.getAnnotation(Description.class);
		if (annotation == null)
			return null;

		String[] descriptions = annotation.value();
		if (descriptions.length != 0) {

			String del = "";
			for (String description : descriptions) {
				sb.append(del);
				sb.append(description);
				del = "\n";
			}
		}

		if (sb.length() == 0)
			return null;

		return sb.toString();
	}

	private String getDescription(AnnotatedElement[] elements) {
		for (AnnotatedElement e : elements) {
			String description = getDescription(e);
			if (description != null)
				return description;
		}
		return null;
	}

	/*
	 * There is a response for each method based on its return variable and a
	 * response for each exception.
	 * 
	 */
	private void doResponses(OperationObject operationObject, Function function)
			throws Exception {
		doResponse(operationObject, function.method.getAnnotatedReturnType());
		for (AnnotatedType annotatedException : function.method
				.getAnnotatedExceptionTypes()) {
			doResponse(operationObject, annotatedException, annotatedException);
		}
	}

	/*
	 * Create a response for a type. Type is either the return type or a
	 * declared exception
	 */
	@SuppressWarnings("unchecked")
	private void doResponse(OperationObject operationObject, AnnotatedType at,
			AnnotatedElement... elements) throws Exception {

		ResponseObject responseObject = new ResponseObject();

		responseObject.description = getDescription(at);
		responseObject.schema = new SchemaObject();

		doSchema(responseObject.schema, at, elements);

		String code = "200";

		if (at.getType() instanceof Class) {
			Class<?> type = (Class<?>) at.getType();
			if (RESTResponse.class.isAssignableFrom((Class<?>) at.getType())) {

				Class<RESTResponse> responseType = (Class<RESTResponse>) at
						.getType();
				RESTResponse restResponse = responseType.newInstance();

				if (restResponse.getStatusCode() != 0)
					code = Integer.toString(restResponse.getStatusCode());

				if (restResponse.getContentType() != null)
					operationObject.produces.add(restResponse.getContentType());

				RestMapper.getPublicFields(responseType, RESTResponse.class)
						.forEach(f -> {
							try {
								String name = RestMapper.decode(f.getName());
								HeaderObject headerObject = new HeaderObject();
								headerObject.collectionFormat = null;
								headerObject.description = getDescription(f);
								doSchema(headerObject, f.getAnnotatedType());
								responseObject.headers.put(name, headerObject);
							} catch (Exception e) {
								log.error(
										"Failed to create response object for "
												+ operationObject.operationId
												+ ":" + type.getName(),
										e);
							}
						});

			} else {
				if (Throwable.class.isAssignableFrom(type))
					code = Integer.toString(ResponseException
							.getStatusCode((Class<? extends Throwable>) type));
			}
		}

		while (operationObject.responses.containsKey(code))
			code += "_";

		operationObject.responses.put("" + code, responseObject);
	}

	private void doSchema(PrimitiveSchema schema, AnnotatedType at,
			AnnotatedElement... elements) throws Exception {
		SchemaObject schemaObject = toSchemaObject(at, elements);
		copy(schemaObject, schema);
	}

	private void copy(DTO src, DTO dest) {
		for (Field field : src.getClass().getFields()) {
			if (Modifier.isStatic(field.getModifiers()))
				continue;

			try {
				Field other = dest.getClass().getField(field.getName());
				field.set(dest, other.get(src));
			} catch (Exception e) {

			}
		}

	}

	private void doQueryParameters(Function f, OperationObject operationObject)
			throws URISyntaxException {
		if (f.hasRequestParameter) {

			AnnotatedType request = f.method.getAnnotatedParameterTypes()[0];
			@SuppressWarnings("unchecked")
			Class<? extends RESTRequest> requestClass = (Class<? extends RESTRequest>) request
					.getType();

			RestMapper.getPublicMethod(requestClass, RESTRequest.class)//
					.filter(m -> !m.getName().startsWith("_"))//
					.forEach(method -> {
						try {
							ParameterObject parameterObject = new ParameterObject();
							parameterObject.name = RestMapper
									.decode(method.getName());
							if ( Character
									.isUpperCase(method.getName().charAt(0))) {
								parameterObject.in = In.header;
								parameterObject.name = parameterObject.name.toUpperCase();
							} else {
								parameterObject.in = In.query;
							}
							
							parameterObject.description = getDescription(
									method);
							parameterObject.deprecated = getDeprecated(method);
							parameterObject.required = getRequired(method);
							doSchema(parameterObject.schema,
									method.getAnnotatedReturnType());
							operationObject.parameters.add(parameterObject);
						} catch (Exception e) {
							log.error(
									"Failed to create query parameter object for "
											+ operationObject.operationId + ":"
											+ method.getName(),
									e);
						}
					});
		}
	}

	private void doPathParameters(Function f, OperationObject operationObject)
			throws Exception {
		for (Parameter parameter : f.getParameters()) {
			ParameterObject parameterObject = new ParameterObject();
			parameterObject.name = parameter.getName();
			parameterObject.in = In.path;
			parameterObject.deprecated = getDeprecated(
					parameter.getAnnotatedType());
			parameterObject.description = getDescription(
					parameter.getAnnotatedType());
			parameterObject.required = true;
			operationObject.parameters.add(parameterObject);
			parameterObject.schema = toSchemaObject(parameter.getAnnotatedType(), parameter);
		}
	}

	private <T> T careful(T old, T newer, T deflt) {
		if (newer != null && newer.equals(deflt))
			return old;

		if (old != null && !old.equals(deflt))
			return old;

		return newer;
	}

	private void doRequestBody(Function function,
			OperationObject operationObject) throws Exception {
		if (function.hasPayloadAsParameter) {

			//
			// We have the payload as argument
			//

			RequestBodyObject requestBodyObject = new RequestBodyObject();
			Parameter bodyParameter = function.method
					.getParameters()[function.hasRequestParameter ? 1 : 0];
			requestBodyObject.required = getRequired(bodyParameter);
			doSchema(requestBodyObject.schema,
					bodyParameter.getAnnotatedType());
			operationObject.requestBody = requestBodyObject;
		} else if (function.post != null) {
			assert function.hasRequestParameter;
			assert function.bodyMethod != null;

			RequestBodyObject requestBodyObject = new RequestBodyObject();
			requestBodyObject.schema = toSchemaObject(
					function.bodyMethod.getAnnotatedReturnType(),
					function.bodyMethod);
			operationObject.requestBody = requestBodyObject;
		}
	}

	private boolean doValidators(SchemaObject schema,
			AnnotatedElement... elements) throws Exception {
		for (int i = elements.length - 1; i >= 0; i--) {
			AnnotatedElement element = elements[i];

			switch (schema.type) {
			case ARRAY: {
				ValidatorArray validator = element
						.getAnnotation(ValidatorArray.class);
				if (validator != null) {
					schema.maxItems = careful(schema.maxItems,
							validator.maxItems(), Integer.MAX_VALUE);
					schema.minItems = careful(schema.minItems,
							validator.minItems(), 0);
					schema.uniqueItems = careful(schema.uniqueItems,
							validator.uniqueItems(), false);
				}
			}
				break;
			case BOOLEAN:
				break;
			case NUMBER:
			case INTEGER: {
				ValidatorNumber validator = element
						.getAnnotation(ValidatorNumber.class);
				if (validator != null) {
					schema.exclusiveMaximum = careful(schema.exclusiveMaximum,
							validator.exclusiveMaximum(), false);
					schema.exclusiveMinimum = careful(schema.exclusiveMinimum,
							validator.exclusiveMinimum(), false);
					schema.maximum = careful(schema.maximum,
							validator.maximum(), Double.MAX_VALUE);
					schema.minimum = careful(schema.minimum,
							validator.minimum(), Double.MIN_VALUE);
					schema.multipleOf = careful(schema.multipleOf,
							validator.multipleOf(), 1.0D);
				}
			}
				break;
			case OBJECT: {
				ValidatorObject validator = element
						.getAnnotation(ValidatorObject.class);
				if (validator != null) {
					schema.maxProperties = careful(schema.maxProperties,
							validator.maxProperties(), Integer.MAX_VALUE);
					schema.minProperties = careful(schema.minProperties,
							validator.minProperties(), 0);
				}
			}
				break;
			case STRING: {
				ValidatorString validator = element
						.getAnnotation(ValidatorString.class);
				if (validator != null) {
					schema.enum_ = getEnum(validator.enum_());
					schema.maxLength = careful(schema.maxLength,
							validator.maxLength(), Integer.MAX_VALUE);
					schema.minLength = careful(schema.minLength,
							validator.minLength(), 0);
					schema.pattern = careful(schema.pattern,
							validator.pattern(), "");
				}
			}
				break;
			case NONE:
			default:
				break;
			}
		}
		return false;
	}

	private String[] getEnum(Class<?> enum_) {

		return null;
	}

	// private void learnFromAnnotation(SchemaObject schema, Schema schemaAnn) {
	// schema.description = careful(schema.description,
	// schemaAnn.description());
	// schema.discriminator = careful(schema.discriminator,
	// schemaAnn.discriminator());
	// schema.format = careful(schema.format, schemaAnn.format());
	// schema.readOnly = careful(schema.readOnly, schemaAnn.readOnly());
	// schema.title = careful(schema.title, schemaAnn.title());
	// }

	private SchemaObject toSchemaObject(AnnotatedType annotatedType,
			AnnotatedElement... elements) throws Exception {
		Type type = annotatedType.getType();
		SchemaObject schema;

		if (type instanceof Class<?>) {
			Class<?> clazz = (Class<?>) type;
			schema = toSchemaObject(clazz);
		} else if (annotatedType instanceof AnnotatedParameterizedType) {

			AnnotatedParameterizedType annotatedPtype = (AnnotatedParameterizedType) annotatedType;
			ParameterizedType ptype = (ParameterizedType) annotatedPtype
					.getType();

			Type sub = ptype.getRawType();

			if (sub instanceof Class) {

				Class<?> clazz = (Class<?>) sub;
				schema = toSchemaObject(clazz);

				if (Collection.class.isAssignableFrom(clazz)) {
					schema.type = PrimitiveType.ARRAY;

					schema.items.add(toSchemaObject(annotatedPtype
							.getAnnotatedActualTypeArguments()[0]));
				} else if (clazz.isArray()) {
					schema.type = PrimitiveType.ARRAY;
					schema.items.add(toSchemaObject(annotatedPtype
							.getAnnotatedActualTypeArguments()[0]));
				} else if (Map.class.isAssignableFrom(clazz)) {
					schema.type = PrimitiveType.OBJECT;
				}
			} else {
				// TODO I've no idea, our raw type is another generic type
				schema = new SchemaObject();
				schema.type = PrimitiveType.OBJECT;
			}
		} else {
			schema = new SchemaObject();
			schema.type = PrimitiveType.STRING;
		}
		schema.deprecated = getDeprecated(annotatedType);
		schema.description = getDescription(elements);

		doValidators(schema, annotatedType);
		doValidators(schema, elements);
		return schema;
	}

	private SchemaObject toSchemaObject(Class<?> clazz) throws Exception {
		SchemaObject schema = new SchemaObject();
		if (clazz.isPrimitive())
			clazz = toWrapper(clazz);

		if (clazz == Boolean.class) {
			schema.type = PrimitiveType.BOOLEAN;
		} else if (Number.class.isAssignableFrom(clazz)
				|| clazz == Character.class) {
			schema.type = PrimitiveType.NUMBER;

			if (clazz == Byte.class) {
				schema.type = PrimitiveType.INTEGER;
				schema.format = "byte";
//				schema.minimum = Byte.MIN_VALUE;
//				schema.maximum = Byte.MAX_VALUE;
			} else if (clazz == Short.class || clazz == Integer.class
					|| clazz == Character.class) {
				schema.type = PrimitiveType.INTEGER;
				schema.format = "int32";
//				schema.minimum = Integer.MIN_VALUE;
//				schema.maximum = Integer.MAX_VALUE;
			} else if (clazz == Long.class) {
				schema.type = PrimitiveType.INTEGER;
				schema.format = "int64";
//				schema.minimum = Long.MIN_VALUE;
//				schema.maximum = Long.MAX_VALUE;
			} else if (clazz == Float.class) {
				schema.type = PrimitiveType.NUMBER;
				schema.format = "float";
//				schema.minimum = Float.MIN_VALUE;
//				schema.maximum = Float.MAX_VALUE;
			} else if (clazz == Double.class) {
				schema.type = PrimitiveType.NUMBER;
				schema.format = "double";
//				schema.minimum = Double.MIN_VALUE;
//				schema.maximum = Double.MAX_VALUE;
			}
		} else if (clazz.isArray()) {
			schema.type = PrimitiveType.ARRAY;
			schema.items.add(toSchemaObject(clazz.getComponentType()));

			if (clazz == byte[].class) {
				schema.type = PrimitiveType.STRING;
				schema.format = "binary";
			}

		} else if (Collection.class.isAssignableFrom(clazz)) {
			schema.type = PrimitiveType.ARRAY;
		} else if (Map.class.isAssignableFrom(clazz)) {
			schema.type = PrimitiveType.OBJECT;
		} else if (clazz == Date.class) {
			schema.type = PrimitiveType.STRING;
			schema.format = "date-time";
		} else if (clazz == UUID.class) {
			schema.type = PrimitiveType.STRING;
			schema.format = "uuid";
		} else if (Enum.class.isAssignableFrom(clazz)) {
			schema.type = PrimitiveType.STRING;
			schema.enum_ = clazz.getEnumConstants();
		} else {
			List<String> required = new ArrayList<>();
			for (Field f : clazz.getFields()) {
				if (Modifier.isStatic(f.getModifiers()))
					continue;

				if (f.getDeclaringClass() == Object.class)
					continue;

				schema.properties.put(f.getName(),
						toSchemaObject(f.getAnnotatedType(), f));
				if (f.getAnnotation(Required.class) != null)
					required.add(f.getName());
			}

			if (schema.properties.isEmpty()) {
				schema.type = PrimitiveType.STRING;
			} else {
				schema.type = PrimitiveType.OBJECT;
				if (!required.isEmpty())
					schema.required = required.toArray(DEFLT);
			}
		}
		return schema;
	}

	private Class<?> toWrapper(Class<?> clazz) {
		if (clazz == byte.class)
			return Byte.class;
		if (clazz == short.class)
			return Short.class;
		if (clazz == char.class)
			return Character.class;
		if (clazz == int.class)
			return Integer.class;
		if (clazz == long.class)
			return Long.class;
		if (clazz == float.class)
			return Float.class;
		if (clazz == double.class)
			return Double.class;

		assert clazz == boolean.class;
		return Boolean.class;
	}

	private TransferProtocol getTransferProtocol(String scheme) {
		return TransferProtocol.valueOf(scheme.toLowerCase());
	}

	private URI toUri(String uri) throws URISyntaxException {
		return uri == null || uri.isEmpty() ? null : new URI(uri);
	}
}
