package io.descoped.dc.api.node.builder;


import io.descoped.dc.api.node.ResponsePredicate;

/**
 * Base for the builders of {@link ResponsePredicate} nodes, the predicates an operation's response is checked
 * against.
 */
public abstract class ResponsePredicateBuilder extends LeafNodeBuilder {

    ResponsePredicateBuilder(BuilderType type) {
        super(type);
    }

    abstract static class ResponsePredicateNode extends AbstractBaseNode implements ResponsePredicate {
    }
}
