package io.descoped.dc.api.node;

/**
 * A node that marks an entry's position as completed and publishes the buffered content of the positions that are
 * completed in sequence order.
 */
public interface Publish extends Node {

    /**
     * The expression that gives the position.
     *
     * @return the expression
     */
    String positionVariableExpression();

}
