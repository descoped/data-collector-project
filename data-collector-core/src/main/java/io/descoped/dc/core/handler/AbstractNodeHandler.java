package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.node.Node;

/**
 * The base of the handlers of {@code Node}s: its {@link #execute(ExecutionContext)} joins the global context of the
 * node's flow into the input context, and subclasses call it first.
 *
 * @param <T> the type of node the handler executes
 */
public abstract class AbstractNodeHandler<T extends Node> extends AbstractHandler<T> {

    /**
     * Creates a handler for a node.
     *
     * @param node the node to execute
     */
    protected AbstractNodeHandler(T node) {
        super(node);
    }

    /**
     * Returns an enriched context with global configuration applied (if present)
     *
     * @param context input context
     * @return input context
     */
    @Override
    public ExecutionContext execute(ExecutionContext context) {
        return configureContext(context);
    }
}
