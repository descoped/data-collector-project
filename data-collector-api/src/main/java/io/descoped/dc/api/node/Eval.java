package io.descoped.dc.api.node;

/**
 * A query that evaluates an expression, optionally after binding the result of a sub-query to a variable.
 */
public interface Eval extends Query {

    /**
     * The sub-query whose result is bound to {@link #bind()}.
     *
     * @return the query, or {@code null} if none is set
     */
    Query query();

    /**
     * The name of the variable the sub-query's result is bound to.
     *
     * @return the variable name
     */
    String bind();

}
