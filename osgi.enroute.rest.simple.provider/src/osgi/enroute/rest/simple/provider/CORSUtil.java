package osgi.enroute.rest.simple.provider;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import osgi.enroute.rest.api.CORS;

/**
 * Utility for processing cross-origin requests (CORS).
 */
public class CORSUtil {

    private final static Pattern ACCEPTED_METHOD_NAMES_P = Pattern
            .compile("(?<conf>.*)(?<verb>All|Get|Post|Put|Delete|Option|Head)(?<path>.*)(?<cardinality>\\d*)");

    static class CORSConfig extends org.osgi.dto.DTO {
        /**
         * The Verb to which this configuration corresponds.
         */
        Verb verb;

        /**
         * Given an origin, return an Optional<String> containing the value
         * of the allowed origin.
         */
        java.util.function.Function<String, Optional<String>> allowOrigin;

        /**
         * True if the allowed origin is determined dynamically, false otherwise. 
         */
        boolean dynamicOrigin;

        /**
         * Given an origin, return the allowed methods.
         */
        java.util.function.Function<String, String> allowMethods;

        /**
         * Given an origin, a verb, and a list of requested headers, return the allowed headers.
         * The Function + BiFunction provides the equivalent of a TriFunction.
         */
        BiFunction<String, List<String>, String> allowHeaders;
        boolean allowCredentials;
        Optional<String> exposeHeaders;
        Optional<String> maxAge;
    }

    /**
     * A Request is identified as a CORS request simply by determining if there is an
     * "Origin" header. The presence of an "Origin" header indicates that the client is
     * making a cross-origin request. 
     */
    static boolean isCORSRequest(HttpServletRequest rq) {
        Enumeration<String> e = rq.getHeaderNames();
        while (e.hasMoreElements()) {
            if("Origin".equals( e.nextElement() ))
                return true;
        }

        return false;
    }

    static void doCORS(HttpServletRequest rq, HttpServletResponse rsp, CORSConfig config, FunctionGroup fg) {
        Map<String, String> headers = processHeaders( rq, config, fg );
        headers.entrySet().stream()
            .forEach(e -> rsp.setHeader(e.getKey(), e.getValue()));
    }

    /**
     * Main processing algorithm for CORS. Attempts to follow the CORS specification on a per-request,
     * per-endpoint basis.
     */
    private static Map<String, String> processHeaders(HttpServletRequest rq, CORSConfig config, FunctionGroup fg) {
        Map<String, String> corsHeaders = new HashMap<>();
        String origin = rq.getHeader("Origin");
        Optional<String> allowOrigin = config.allowOrigin.apply(origin);
        if (allowOrigin != null && allowOrigin.isPresent()) {
            // This header is set only if the provided Origin is allowed.
            corsHeaders.put("Access-Control-Allow-Origin", allowOrigin.get());
        } else {
            // If the Origin header is not allowed, then the CORS request must fail with a 403
            // TODO: write to log
            throw new SecurityException();
        }

        if (config.allowCredentials) {
            corsHeaders.put("Access-Control-Allow-Credentials", "true");
        }

        if (isPreflight(rq)) {
            String configuredAllowMethods = config.allowMethods.apply(origin);
            String allowMethods = CORS.ALL.equals(configuredAllowMethods) ? fg.getOptions() : configuredAllowMethods;
            corsHeaders.put("Access-Control-Allow-Methods", allowMethods);
            String requestedHeadersString = rq.getHeader("Access-Control-Request-Headers");
            String[] requestedHeadersArray = requestedHeadersString != null ? rq.getHeader("Access-Control-Request-Headers").split(", ") : new String[]{};
            List<String> requestedHeaders = Arrays.asList(requestedHeadersArray);
            corsHeaders.put("Access-Control-Allow-Headers", config.allowHeaders.apply(origin, requestedHeaders));
            if (config.maxAge !=null && config.maxAge.isPresent())
                corsHeaders.put("Access-Control-Max-Age", config.maxAge.get());
        } else if (config.exposeHeaders != null && config.exposeHeaders.isPresent()) {
            // Do this only in the case that a request is not a pre-flight request and there are headers to expose
            corsHeaders.put("Access-Control-Expose-Headers", config.exposeHeaders.get());
        }

        return corsHeaders;
    }

    private static boolean isPreflight(HttpServletRequest rq) {
        // A pre-flight request must be made using the "OPTION" method.
        // TODO: Here we are using string matching ("OPTIONS" and not "Options" or "options").
        //       Consider if we should allow use of non-compliant versions as well.
        if (!"OPTIONS".equals(rq.getMethod()))
            return false;

        // A pre-flight request must have a non-empty Access-Control-Request-Method header
        String acrm = rq.getHeader("Access-Control-Request-Method");
        if (acrm == null || acrm.isEmpty())
            return false;

        // The above two conditions are met, so this qualifies as a pre-flight request.
        return true;
    }

    /**
     * The Method config takes precedence over the class config.
     */
    static CORSConfig parseCORS(Object target, Method method, String name, Verb verb, int cardinality) {
        CORS classCORS = target.getClass().getAnnotation(CORS.class);
        CORS methodCORS = method.getAnnotation(CORS.class);

        if (classCORS != null && methodCORS == null)
                return convert(target, name, verb, cardinality, classCORS);

        return convert(target, name, verb, cardinality, methodCORS);
    }

    @SuppressWarnings( "unchecked" )
    private static CORSConfig convert(Object target, String name, Verb verb, int cardinality, CORS cors) {

        if (cors == null)
            return null;

        CORSConfig config = new CORSConfig();

        // Process "Origin"
        if (CORS.DYNAMIC.equals(cors.origin())) {
            config.dynamicOrigin = true;
            Method m = findMethod("allowOrigin", name, verb, cardinality, target);

            if (m != null) {
                try {
                    m.setAccessible(true);
                    config.allowOrigin = (java.util.function.Function<String, Optional<String>>)m.invoke(target);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace(); // Log this properly
                }
            } else {
                // CORS is configured as DYNAMIC, but no method was found
                // TODO: log this
                config.dynamicOrigin = false;
                config.allowOrigin = origin -> Optional.empty();
            }
        } else {
            config.dynamicOrigin = false;
            config.allowOrigin = 
                    cors.origin().isEmpty() ? 
                            origin -> Optional.empty() : 
                            origin -> Optional.ofNullable(cors.origin());
        }

        // Process "Access-Control-Allow-Methods"
        if (CORS.DYNAMIC.equals(cors.origin())) {
            Method m = findMethod("allowMethods", name, verb, cardinality, target);

            if (m != null) {
                try {
                    m.setAccessible(true);
                    config.allowMethods = (java.util.function.Function<String, String>)m.invoke(target);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace(); // Log this properly
                }
            } else {
                // CORS is configured as DYNAMIC, but no method was found
                // TODO: log this
                config.allowMethods = origin -> "";
            }
        } else {
            config.allowMethods = methods -> Arrays.stream(cors.allowMethods()).collect(Collectors.joining(", "));
        }

        // Process "Access-Control-Allow-Headers"
        if (CORS.DYNAMIC.equals(cors.origin())) {
            Method m = findMethod("allowHeaders", name, verb, cardinality, target);

            if (m != null) {
                try {
                    m.setAccessible(true);
                    config.allowHeaders = (BiFunction<String,List<String>,String>)m.invoke(target);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace(); // Log this properly
                }
            } else {
                // CORS is configured as DYNAMIC, but no method was found
                // TODO: log this
                config.allowHeaders = (origin, headers) -> "";
            }
        } else {
            config.allowHeaders = (origin, headers) -> Arrays.stream(cors.allowHeaders()).collect(Collectors.joining(", "));
        }

        config.allowCredentials = cors.allowCredentials();
        if (cors.exposeHeaders() != null && cors.exposeHeaders().length > 0)
            config.exposeHeaders = Optional.of(Arrays.stream(cors.exposeHeaders()).collect( Collectors.joining(", ")));
        if (cors.maxAge() >= 0)
            config.maxAge = Optional.ofNullable(String.valueOf(cors.maxAge()));

        config.verb = verb;

        return config;
    }

    private static class CORSMethod {
        Optional<Verb> verb;
        Optional<Integer> cardinality;
        String name;

        @Override
        public String toString() {
            return new StringBuilder()
                    .append(verb.isPresent() ? verb.get() : "")
                    .append(name)
                    .append(cardinality.isPresent() ? "/" + cardinality.toString() : "")
                    .toString();
        }
    }

    private static Method findMethod(String name, String endpoint, Verb verb, int cardinality, Object target) {
        Method foundMethod = null;

        try {
            // For example: user (could include getuser/0, postuser/2 etc.)
            Method groupGeneralMethod = null;
            // For example: user/0 (could include getuser/0, postuser/0 etc.)
            Method groupSpecificMethod = null;
            // For example: getuser (could include getuser/0, getuser/1 etc.)
            Method endpointGeneralMethod = null;
            // For example: getuser/0
            Method endpointSpecificMethod = null;

            for (Method m : target.getClass().getDeclaredMethods()) {
                if (!m.getName().startsWith(name))
                    continue;

                // Is it a general one for the set of groups?
                // Is it one very specific to that group?
                CORSMethod cm = parseMethodName(m.getName(), verb, cardinality);
                if (cm != null) {
                    if (!cm.verb.isPresent() && !cm.cardinality.isPresent())
                        groupGeneralMethod = m;
                    else if (!cm.verb.isPresent() && cm.cardinality.isPresent())
                        groupSpecificMethod = m;
                    else if (cm.verb.isPresent() && !cm.cardinality.isPresent())
                        endpointGeneralMethod = m;
                    else
                        endpointSpecificMethod = m;
                }
            }

            if (endpointSpecificMethod != null )
                return endpointSpecificMethod;

            if (endpointGeneralMethod != null)
                return endpointGeneralMethod;

            if (groupSpecificMethod != null)
                return groupSpecificMethod;

            if (groupGeneralMethod != null )
                return groupGeneralMethod;

            if (foundMethod == null)
                foundMethod = target.getClass().getDeclaredMethod(name);

        } catch (Exception e) {
            // Do nothing
            e.toString();
        }

        return foundMethod;
    }

    private static CORSMethod parseMethodName(String methodName, Verb verb, int cardinality) {
        CORSMethod m = new CORSMethod();
        Matcher matcher = ACCEPTED_METHOD_NAMES_P.matcher(methodName);
        if (!matcher.lookingAt())
            return null;

        m.verb = Optional.ofNullable(matcher.group("verb"))
                .filter(v -> !v.isEmpty() && !"All".equals(v))
                .map(v -> v.toLowerCase())
                .map(v -> Verb.valueOf(v));
        m.cardinality = Optional.ofNullable(matcher.group("cardinality"))
                .filter(c -> !c.isEmpty())
                .map(c -> Integer.valueOf(c));
        m.name = matcher.group("path").toLowerCase();

        // TODO: Remove this block if the regex can be fixed to actually work properly.
        //       Currently it is not capturing the last numeric block.
        {
            String card = "";
            while (Character.isDigit(m.name.charAt(m.name.length() - 1))) {
                card = m.name.charAt(m.name.length() - 1) + card;
                m.name = m.name.substring(0, m.name.length() - 1);
            }

            if (!card.isEmpty())
                m.cardinality = Optional.of(card).map(c -> Integer.valueOf(c));
        }

        if (m.verb.isPresent() && m.verb.get() != verb)
            return null;

        if (m.cardinality.isPresent() && m.cardinality.get().intValue() != cardinality)
            return null;

        return m;
    }
}
