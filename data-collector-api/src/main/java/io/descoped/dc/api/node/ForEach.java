package io.descoped.dc.api.node;

import java.util.List;

/**
 * A node that splits the response into a list and runs its steps for each item.
 */
public interface ForEach extends Node {

    /**
     * The query that splits the response into a list.
     *
     * @return the query
     */
    Query splitToListQuery();

    /**
     * The steps run for each item.
     *
     * @return the steps
     */
    List<? extends Node> steps();

}
