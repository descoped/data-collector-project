package io.descoped.dc.api.node;

/**
 * A response predicate that is true when a query on the response body gives a string literal.
 */
public interface BodyContains extends ResponsePredicate {

    /**
     * The query on the response body.
     *
     * @return the query
     */
    Query getQuery();

    /**
     * The string the query result must equal.
     *
     * @return the string
     */
    String getEqualToStringLiteral();

}
