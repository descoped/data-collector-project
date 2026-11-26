package io.descoped.dc.application.controller;

import java.util.List;
import java.util.Objects;

/**
 * A path template, split into its elements.
 */
public class PathParser {

    private final UriTemplate template;

    private PathParser(String template) {
        this.template = new UriTemplate(template);
    }

    /**
     * Parses a path template.
     *
     * @param template the template, with {@code {variable}} elements
     * @return the parser
     */
    public static PathParser create(String template) {
        return new PathParser(template);
    }

    /**
     * The template's elements.
     *
     * @return the elements
     */
    public List<String> elements() {
        return template.elements();
    }

    /**
     * Matches a request path against the template.
     *
     * @param url the request path
     * @return the bindings
     */
    public PathBindings path(String url) {
        UriTemplate elements = new UriTemplate(url);
        return new PathBindings(template, elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathParser that = (PathParser) o;
        return Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(template);
    }
}
