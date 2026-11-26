package io.descoped.dc.api.node.builder;

import io.descoped.dc.api.node.Query;

/**
 * Base for the builders of {@link Query} nodes, the queries that read values from a response or a document.
 */
public abstract class QueryBuilder extends NodeBuilder {

    QueryBuilder(BuilderType type) {
        super(type);
    }

    abstract static class QueryNode extends AbstractBaseNode implements Query {
    }
}
