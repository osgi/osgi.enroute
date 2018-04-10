package org.osgi.enroute.examples.microservice.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.osgi.service.component.annotations.ServiceScope.PROTOTYPE;
import static org.osgi.util.converter.ConverterFunction.CANNOT_HANDLE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.JsonWriter;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsMediaType;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TypeReference;

@Component(scope = PROTOTYPE)
@JaxrsExtension
@JaxrsMediaType(APPLICATION_JSON)
public class JsonpConvertingPlugin<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {

    private final Converter converter = Converters.newConverterBuilder()
            .rule(JsonValue.class, this::toJsonValue)
            .rule(this::toScalar)
            .build();

    private JsonValue toJsonValue(Object value, Type targetType) {
        if (value == null) {
           return JsonValue.NULL;
        } else if (value instanceof String) {
            return Json.createValue(value.toString());
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (value instanceof Number) {
            Number n = (Number) value;
            if (value instanceof Float || value instanceof Double) {
                return Json.createValue(n.doubleValue());
            } else if (value instanceof BigDecimal) {
                return Json.createValue((BigDecimal) value);
            } else if (value instanceof BigInteger) {
                return Json.createValue((BigInteger) value);
            } else {
                return Json.createValue(n.longValue());
            }
        } else if (value instanceof Collection || value.getClass().isArray()) {
            return toJsonArray(value);
        } else {
            return toJsonObject(value);
        }
    }

    private JsonArray toJsonArray(Object o) {
        List<?> l = converter.convert(o).to(List.class);
    
        JsonArrayBuilder builder = Json.createArrayBuilder();
        l.forEach(v -> builder.add(toJsonValue(v, JsonValue.class)));
        return builder.build();
    }

    private JsonObject toJsonObject(Object o) {

        Map<String, Object> m = converter.convert(o).to(new TypeReference<Map<String, Object>>(){});

        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        m.entrySet().stream().forEach(e -> jsonBuilder.add(e.getKey(), toJsonValue(e.getValue(), JsonValue.class)));
        return jsonBuilder.build();
    }

    private Object toScalar(Object o, Type t) {

        if (o instanceof JsonNumber) {
            JsonNumber jn = (JsonNumber) o;
            return converter.convert(jn.bigDecimalValue()).to(t);
        } else if (o instanceof JsonString) {
            JsonString js = (JsonString) o;
            return converter.convert(js.getString()).to(t);
        } else if (o instanceof JsonValue) {
            JsonValue jv = (JsonValue) o;
            if (jv.getValueType() == ValueType.NULL) {
                return null;
            } else if (jv.getValueType() == ValueType.TRUE) {
                return converter.convert(Boolean.TRUE).to(t);
            } else if (jv.getValueType() == ValueType.FALSE) {
                return converter.convert(Boolean.FALSE).to(t);
            }
        }
        return CANNOT_HANDLE;
    }

    @Override
    public boolean isWriteable(Class<?> c, Type t, Annotation[] a, MediaType mediaType) {
        return APPLICATION_JSON_TYPE.isCompatible(mediaType) || mediaType.getSubtype().endsWith("+json");
    }

    @Override
    public boolean isReadable(Class<?> c, Type t, Annotation[] a, MediaType mediaType) {
        return APPLICATION_JSON_TYPE.isCompatible(mediaType) || mediaType.getSubtype().endsWith("+json");
    }

    @Override
    public void writeTo(T o, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, java.lang.Object> arg5, OutputStream out)
            throws IOException, WebApplicationException {

        JsonValue jv = converter.convert(o).to(JsonValue.class);

        try (JsonWriter jw = Json.createWriter(out)) {
            jw.write(jv);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T readFrom(Class<T> arg0, Type arg1, Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4,
            InputStream in) throws IOException, WebApplicationException {

        try (JsonReader jr = Json.createReader(in)) {
            JsonStructure read = jr.read();
            return (T) converter.convert(read).to(arg1);
        }
    }
}
