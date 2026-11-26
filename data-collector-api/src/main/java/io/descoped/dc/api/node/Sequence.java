package io.descoped.dc.api.node;

/**
 * A node that splits the response into a list and gives the expected position of each item.
 */
public interface Sequence extends Node {

    /**
     * The query that splits the response into a list.
     *
     * @return the query
     */
    Query splitToListQuery();

    /**
     * The query that gives an item's position.
     *
     * @return the query
     */
    Query expectedQuery();

}
