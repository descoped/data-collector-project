package io.descoped.dc.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The variables an operation returns, by name, in the order they were added.
 */
public class ReturnVariablesState {

    private final Map<String, Object> variableMap = new LinkedHashMap<>();

    /**
     * Creates an empty state.
     */
    public ReturnVariablesState() {
        // no variables until they are added
    }

    /**
     * Adds a variable without a value.
     *
     * @param name the name
     */
    public void add(String name) {
        variableMap.put(name, null);
    }

    /**
     * Adds a variable.
     *
     * @param name  the name
     * @param value the value
     */
    public void add(String name, Object value) {
        variableMap.put(name, value);
    }

    /**
     * The variable names.
     *
     * @return the names
     */
    public Set<String> keySet() {
        return variableMap.keySet();
    }

    /**
     * The value of a variable.
     *
     * @param key the name
     * @return the value, or {@code null} if the variable has none or is absent
     */
    public Object value(String key) {
        return variableMap.get(key);
    }

    /**
     * The variables as {@code name=value} pairs, separated by commas.
     *
     * @return the text
     */
    public String asString() {
        return variableMap.entrySet().stream().map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue())).collect(Collectors.joining(","));
    }
}
