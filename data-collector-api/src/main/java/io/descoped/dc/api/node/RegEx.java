package io.descoped.dc.api.node;

/**
 * A regular expression applied to the result of a sub-query.
 */
public interface RegEx extends Query {

    /**
     * The sub-query whose result the regular expression is applied to.
     *
     * @return the query
     */
    Query query();

}
