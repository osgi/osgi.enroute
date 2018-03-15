package osgi.enroute.rest.simple.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import osgi.enroute.rest.api.CORS;

/**
 * Groups Functions by "endpoint", i.e. excludes the method.
 * 
 * Used as an index for responding quickly to OPTIONS requests.
 */
public class FunctionGroup {
    final String name;
    final Function get;
    final Function post;
    final Function put;
    final Function delete;
    final java.util.function.Function<Verb, CORSUtil.CORSConfig> optionsConfig;
    final String options;

    FunctionGroup(
            String name,
            Function get,
            Function post,
            Function put,
            Function delete) {
        this.name = name;
        this.get = get;
        this.post = post;
        this.put = put;
        this.delete = delete;
        this.options = "OPTIONS";
        this.optionsConfig = v -> cors(get(), put(), post(), delete(), options, v);
    }

    FunctionGroup(FunctionGroup existing, Function f, Verb verb) {
        this.name = existing.name;
        this.get = verb == Verb.get ? f : existing.get;
        this.post = verb == Verb.post ? f : existing.post;
        this.put = verb == Verb.put ? f : existing.put;
        this.delete = verb == Verb.delete ? f : existing.delete;

        List<String> allowed = new ArrayList<>();
        allowed.add("OPTIONS");
        this.get().ifPresent(h -> {allowed.add("GET"); allowed.add("HEAD");});
        this.post().ifPresent(h -> allowed.add("POST"));
        this.put().ifPresent(h -> allowed.add("PUT"));
        this.delete().ifPresent(h -> allowed.add("DELETE"));
        this.options = allowed.stream().collect( Collectors.joining(", "));
        this.optionsConfig = v -> cors(get(), put(), post(), delete(), options, v);
    }

    public Optional<Function> get() {
        return Optional.ofNullable(get);
    }

    public Optional<Function> put() {
        return Optional.ofNullable(put);
    }

    public Optional<Function> post() {
        return Optional.ofNullable(post);
    }

    public Optional<Function> delete() {
        return Optional.ofNullable(delete);
    }

    public String getGroupName() {
        return name;
    }

    public String getOptions() {
        return options;
    }

    /**
     * Helper method to locate a Function corresponding to a Verb.
     */
    public Optional<Function> hasVerb(String httpMethod) {
        Verb v;
        try {
            v = Verb.valueOf(httpMethod.toLowerCase());
        } catch (Exception e) {
            return Optional.empty();
        }

        switch (v) {
            case get :
                return get();

            case put :
                return put();

            case post :
                return post();

            case delete :
                return delete();

            default :
                return Optional.empty();
        }
    }

    /**
     * Helper method to help locate a Function quicker.
     * Given a Function, returns the Verb for that function 
     * if it exists in this FunctionGroup.
     */
    public Verb hasFunction(Function f) {
        if (get == f)
            return Verb.get;

        if (post == f)
            return Verb.post;

        if (put == f)
            return Verb.put;

        if (delete == f)
            return Verb.delete;

        return null;
    }

    public java.util.function.Function<Verb, CORSUtil.CORSConfig> optionsConfig() {
        return verb -> optionsConfig.apply(verb);
    }

    /**
     * Helper method to show if this FunctionGroup has any Functions 
     * included.
     */
    public boolean hasFunction() {
        return
                ((get != null) ||
                (post != null) ||
                (put != null)  ||
                (delete != null));
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Preflight requests are a bit tricker than "normal" requests, requiring this
     * extra bit of complexity, unfortunately.
     */
    private static CORSUtil.CORSConfig cors(
            Optional<Function> get, 
            Optional<Function> put, 
            Optional<Function> post, 
            Optional<Function> delete, 
            String options,
            Verb verb) {
        CORSUtil.CORSConfig config = new CORSUtil.CORSConfig();
        config.verb = Verb.option;
        config.dynamicOrigin = 
                dynamicOrigin(get) ||
                dynamicOrigin(put) ||
                dynamicOrigin(post) ||
                dynamicOrigin(delete);
        // Perhaps not the "right" thing to do, but too complicated otherwise.
        config.allowOrigin = origin -> Optional.of("*");
        config.allowMethods = origin -> {
            List<String> allowed = Stream.of(get, put, post, delete)
                    .map(m -> allowedMethodFor(origin, m))
                    .filter(o -> !o.isEmpty())
                    .collect(Collectors.toList());
            if (!allowed.isEmpty())
                allowed.add(0, "OPTIONS");
            return allowed.stream().collect(Collectors.joining(", "));
        };
        config.allowHeaders = allowedHeaders(get, put, post, delete, verb);
        config.maxAge = maxAge( get, put, post, delete, verb );
        return config;
    }

    private static boolean dynamicOrigin(Optional<Function> f) {
        return f
            .filter(m -> m.cors != null)
            .map(m -> m.cors.dynamicOrigin)
            .orElse(false);
    }

    private static String allowedMethodFor(String origin, Optional<Function> f) {
        return f
                .filter(m -> m.cors != null)
                .map(m -> m.cors)
                .map(c -> c.allowOrigin.apply(origin))
                .flatMap(opt -> opt)
                .filter(o -> CORS.ALL.equals(o) || origin.equals(o))
                .map(o -> f.get().verb.name().toUpperCase())
                .map(v -> "GET".equals(v) ? "GET, HEAD" : v)
                .orElse("");
    }

    private static BiFunction<String, List<String>, String> allowedHeaders(
            Optional<Function> get, 
            Optional<Function> put, 
            Optional<Function> post, 
            Optional<Function> delete, 
            Verb v) {
        Optional<Function> f = null;
        switch(v)
        {
            case get :
                f = get;
                break;

            case put :
                f = put;
                break;

            case post :
                f = post;
                break;

            case delete :
                f = delete;
                break;

            default :
                f = Optional.empty();
                break;
        }
        
        return f.
                filter(m -> m.cors != null)
                .map(m -> m.cors)
                .map(c -> c.allowHeaders)
                .orElse((origin, requested) -> "");
    }

    private static Optional<String> maxAge(
            Optional<Function> get, 
            Optional<Function> put, 
            Optional<Function> post, 
            Optional<Function> delete, 
            Verb v) {
        Optional<Function> f = null;
        switch(v)
        {
            case get :
                f = get;
                break;

            case put :
                f = put;
                break;

            case post :
                f = post;
                break;

            case delete :
                f = delete;
                break;

            default :
                f = Optional.empty();
                break;
        }

        return f
            .filter(m -> m.cors != null)
            .map(m -> m.cors.maxAge)
            .orElse(Optional.empty());
    }
}
