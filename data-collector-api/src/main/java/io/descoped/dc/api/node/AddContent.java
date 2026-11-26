package io.descoped.dc.api.node;

import java.util.Map;

/**
 * A node that adds the current entry's content to the content stream buffer under a position.
 */
public interface AddContent extends Node {

    /**
     * The expression that gives the entry's position.
     *
     * @return the expression
     */
    String positionVariableExpression();

    /**
     * The key the content is stored under.
     *
     * @return the content key
     */
    String contentKey();

    /**
     * The state stored with the content, by key; the values may be expressions.
     *
     * @return the state, empty when none is set
     */
    Map<String, Object> state();

}
