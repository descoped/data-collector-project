package io.descoped.dc.api.node;

/**
 * The condition that ends a pagination.
 */
public interface Condition extends Base {

    /**
     * The condition's identifier: a variable name or an expression, as the condition type reads it.
     *
     * @return the identifier
     */
    String identifier();

}
