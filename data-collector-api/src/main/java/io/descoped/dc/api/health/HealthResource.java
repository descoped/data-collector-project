package io.descoped.dc.api.health;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;

/**
 * A section of the health output.
 */
public interface HealthResource {

    /**
     * Whether the resource is up; the service is up when every resource is.
     *
     * @return whether it is up, or empty if the resource does not count
     */
    Optional<Boolean> isUp();

    /**
     * The name of the section.
     *
     * @return the name
     */
    String name();

    /**
     * Whether {@link #resource()} is a list, rendered as a JSON array.
     *
     * @return {@code true} for a list
     */
    boolean isList();

    /**
     * Whether the section is rendered for the request's query parameters.
     *
     * @param queryParams the query parameters
     * @return {@code true} if it is rendered
     */
    boolean canRender(Map<String, Deque<String>> queryParams);

    /**
     * The content of the section, converted to JSON.
     *
     * @return the content
     */
    Object resource();

}
