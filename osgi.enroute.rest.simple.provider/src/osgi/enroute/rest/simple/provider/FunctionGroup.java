package osgi.enroute.rest.simple.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public String toString() {
        return name;
    }
}
