package io.descoped.dc.api;

import io.descoped.dc.api.context.ExecutionContext;

/**
 * Something that executes on an execution context.
 */
public interface Execution {

    /**
     * Executes on the input.
     *
     * @param context the input
     * @return the output
     */
    ExecutionContext execute(ExecutionContext context);

}
