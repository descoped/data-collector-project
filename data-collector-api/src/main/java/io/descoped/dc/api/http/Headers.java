package io.descoped.dc.api.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import static java.util.Objects.requireNonNull;

/**
 * HTTP headers: each name with its values, in the order they were added. Names are case sensitive.
 */
public class Headers {

    @JsonProperty("Headers") /* use @JsonWrapper(prefix) in serializer */ Map<String, List<String>> headers = new LinkedHashMap<>();

    /**
     * Creates empty headers.
     */
    public Headers() {
    }

    /**
     * Creates headers with the entries of a map.
     *
     * @param headersMap the names and their values
     */
    public Headers(Map<String, List<String>> headersMap) {
        headers.putAll(headersMap);
    }

    /**
     * Adds a value to a header.
     *
     * @param name  the name
     * @param value the value
     */
    public void put(String name, String value) {
        headers.computeIfAbsent(name, values -> new ArrayList<>()).add(value);
    }

    /**
     * The first value of a header.
     *
     * @param name the name
     * @return the value, or empty if the header is absent
     */
    public Optional<String> firstValue(String name) {
        return allValues(name).stream().findFirst();
    }

    /**
     * The first value of a header, as a number.
     *
     * @param name the name
     * @return the value, or empty if the header is absent
     * @throws NumberFormatException if the value is not a number
     */
    public OptionalLong firstValueAsLong(String name) {
        return allValues(name).stream().mapToLong(Long::valueOf).findFirst();
    }

    /**
     * The values of a header.
     *
     * @param name the name
     * @return the values, empty if the header is absent
     */
    public List<String> allValues(String name) {
        requireNonNull(name);
        List<String> values = asMap().get(name);
        return values != null ? values : List.of();
    }

    /**
     * The headers as a map.
     *
     * @return the backing map, not a copy
     */
    public Map<String, List<String>> asMap() {
        return headers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Headers headers1 = (Headers) o;
        return Objects.equals(headers, headers1.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers);
    }

    @Override
    public String toString() {
        return "Headers{" +
                "headers=" + headers +
                '}';
    }
}
