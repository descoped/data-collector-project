package io.descoped.dc.api.node.builder;

import io.descoped.dc.api.node.Condition;

/**
 * Base for the builders of {@link Condition} nodes, the conditions a {@code Paginate} node stops on.
 */
public abstract class ConditionBuilder extends NodeBuilder {

    ConditionBuilder(BuilderType type) {
        super(type);
    }

    abstract static class ConditionNode extends AbstractBaseNode implements Condition {
    }
}
