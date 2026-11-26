package io.descoped.dc.application.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A request path matched against a path template.
 */
public class PathBindings {

    private final UriTemplate templateElements;
    private final UriTemplate pathElements;

    /**
     * Creates the bindings.
     *
     * @param template the template
     * @param path     the request path
     */
    public PathBindings(UriTemplate template, UriTemplate path) {
        this.templateElements = template;
        this.pathElements = path;
    }

    /**
     * The template's variables with the request path's values.
     *
     * @return the values by variable name
     */
    public Map<String, String> extractVariables() {
        Map<String, String> variables = new LinkedHashMap<>();
        Set<String> variableNames = templateElements.variableNames();
        for (String variableName : variableNames) {
            Integer variableIndex = templateElements.variableIndex(variableName);
            String variableValue = pathElements.elements().get(variableIndex);
            variables.put(variableName, variableValue);
        }
        return variables;
    }

    UriTemplate template() {
        return templateElements;
    }

    UriTemplate request() {
        return pathElements;
    }

    boolean isVariableExpression(String element) {
        return element.startsWith("{") && element.endsWith("}");
    }

    /**
     * Whether the request path matches the template; a variable matches any element.
     *
     * @return true if it matches
     */
    public boolean isSatisfied() {
        if (pathElements.size() > templateElements.size()) {
            return false;
        }
        // iterate pathElements and compare ordered element name. Skip template variables with path element, since they can be anything.
        for (int i = 0; i < pathElements.elements().size(); i++) {
            String templateElement = templateElements.elements().get(i);
            String pathElement = pathElements.elements().get(i);
            if (isVariableExpression(templateElement)) {
                continue;
            }
            if (!pathElement.equals(templateElement)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The number of elements in the request path.
     *
     * @return the number
     */
    public int length() {
        return pathElements.size();
    }
}
