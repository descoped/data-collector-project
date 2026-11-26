package io.descoped.dc.core.executor;

import io.descoped.dc.api.Execution;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.core.handler.Handlers;

/**
 * Runs a node with the handler made for it.
 */
public class Executor {

    private Executor() {
    }

    /**
     * Creates the handler for the node.
     *
     * @param node the node
     * @param <N>  the node's type
     * @return the node's handler
     */
    public static <N extends Base> Execution instanceOf(N node) {
        return Handlers.createHandlerFor(node);
    }

    /**
     * Creates the handler for the node and executes it with the input.
     *
     * @param node  the node
     * @param input the input context
     * @param <N>   the node's type
     * @return the output context
     */
    public static <N extends Base> ExecutionContext execute(N node, ExecutionContext input) {
        Execution executionHandler = Handlers.createHandlerFor(node);
        return executionHandler.execute(input);
    }

}
