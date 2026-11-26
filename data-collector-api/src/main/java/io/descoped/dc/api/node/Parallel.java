package io.descoped.dc.api.node;

import java.util.List;
import java.util.Set;

/**
 * A node that splits the response into a list and runs its steps for each item in parallel, then publishes the
 * positions.
 */
public interface Parallel extends Node {

    /**
     * The query that splits the response into a list.
     *
     * @return the query
     */
    Query splitQuery();

    /**
     * The names of the variables set for each item.
     *
     * @return the names
     */
    Set<String> variableNames();

    /**
     * The query of a variable set for each item.
     *
     * @param name the variable name
     * @return the query, or {@code null} if there is no such variable
     */
    Query variable(String name);

    /**
     * The steps run for each item.
     *
     * @return the steps
     */
    List<Node> steps();

    /**
     * The node that publishes the positions.
     *
     * @return the node, or {@code null} if none is set
     */
    Publish publish();

}
