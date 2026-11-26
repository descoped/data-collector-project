package io.descoped.dc.api.node;

/**
 * A query on a document or a value.
 */
public interface Query extends Base {

    /**
     * The query's expression.
     *
     * @return the expression
     */
    String expression();

}
