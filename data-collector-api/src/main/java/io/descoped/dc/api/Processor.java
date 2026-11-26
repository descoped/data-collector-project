package io.descoped.dc.api;

import io.descoped.dc.api.context.ExecutionContext;

/**
 * Custom code that a {@code Process} node runs; see {@link ProcessorProvider}.
 */
public interface Processor {

    /**
     * Runs on the input.
     *
     * @param input the input, with the response in its state
     * @return the output, with the variables the processor sets
     */
    ExecutionContext process(ExecutionContext input);

}
