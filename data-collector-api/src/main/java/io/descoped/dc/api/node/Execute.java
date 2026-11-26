package io.descoped.dc.api.node;

import java.util.List;
import java.util.Map;

/**
 * A node that executes another node of the specification, given by its id, with input variables.
 */
public interface Execute extends Node {

    /**
     * The id of the node to execute.
     *
     * @return the id
     */
    String executeId();

    /**
     * The names of the variables the target needs as input.
     *
     * @return the names
     */
    List<String> requiredInputs();

    /**
     * The queries that give input variables, by variable name.
     *
     * @return the queries
     */
    Map<String, Query> inputVariable();

    /**
     * The node to execute.
     *
     * @return the node
     */
    NodeWithId target();

}
