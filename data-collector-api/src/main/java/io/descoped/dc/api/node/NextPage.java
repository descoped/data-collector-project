package io.descoped.dc.api.node;

import java.util.Map;

/**
 * A node that gives the variables of the next page.
 */
public interface NextPage extends Node {

    /**
     * The queries that give the next page's variables, by variable name.
     *
     * @return the queries
     */
    Map<String, Query> outputs();

}
