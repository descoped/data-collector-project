package io.descoped.dc.application.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A path split into its elements, with the positions of its {@code {variable}} elements.
 */
public class UriTemplate {

    private static final Pattern PATH_GROUPS_PATTERN = Pattern.compile("(?:[^}{/]++|\\{[^}{]++})++");

    private final String path;
    private final List<String> pathElements;
    private final Map<String, Integer> variableNamesAndElementPosition = new LinkedHashMap<>();

    /**
     * Splits a path.
     *
     * @param uri the path
     */
    public UriTemplate(String uri) {
        this.path = uri;
        pathElements = parse(uri);
    }

    private List<String> parse(String uri) {
        List<String> elements = new ArrayList<>();
        Matcher matcher = PATH_GROUPS_PATTERN.matcher(uri);
        int i = 0;
        while (matcher.find()) {
            String element = matcher.group();
            elements.add(element);
            if (element.startsWith("{") && element.endsWith("}")) {
                variableNamesAndElementPosition.put(element.substring(1, element.length() - 1), i);
            }
            i++;
        }
        return elements;
    }

    /**
     * The path.
     *
     * @return the path
     */
    public String path() {
        return path;
    }

    /**
     * The path's elements.
     *
     * @return the elements
     */
    public List<String> elements() {
        return pathElements;
    }

    /**
     * The names of the variables.
     *
     * @return the names
     */
    public Set<String> variableNames() {
        return variableNamesAndElementPosition.keySet();
    }

    /**
     * The position of a variable.
     *
     * @param name the variable's name
     * @return the position, or {@code null}
     */
    public Integer variableIndex(String name) {
        if (!variableNamesAndElementPosition.containsKey(name)) {
            return -1;
        }
        return variableNamesAndElementPosition.get(name);
    }

    /**
     * The number of elements.
     *
     * @return the number
     */
    public int size() {
        return pathElements.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UriTemplate that = (UriTemplate) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
