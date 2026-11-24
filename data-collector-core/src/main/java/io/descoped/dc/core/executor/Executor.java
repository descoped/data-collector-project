package io.descoped.dc.core.executor;

import io.descoped.dc.api.Execution;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.core.handler.Handlers;

public class Executor {

    private Executor() {
    }

    public static <N extends Base> Execution instanceOf(N node) {
        return Handlers.createHandlerFor(node);
    }

    public static <N extends Base> ExecutionContext execute(N node, ExecutionContext input) {
        Execution executionHandler = Handlers.createHandlerFor(node);
        return executionHandler.execute(input);
    }

}
