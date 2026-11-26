package io.descoped.dc.core.executor;

import io.descoped.dc.api.context.ExecutionContext;

import java.util.UUID;

/**
 * What a {@link WorkerObserver} is told about a worker: its id, its specification and its execution context.
 */
public class WorkerObservable {
    private final UUID workerId;
    private final String specificationId;
    private final ExecutionContext context;

    /**
     * Creates the observable.
     *
     * @param workerId        the worker's id
     * @param specificationId the specification's id
     * @param context         the worker's execution context
     */
    public WorkerObservable(UUID workerId, String specificationId, ExecutionContext context) {
        this.workerId = workerId;
        this.specificationId = specificationId;
        this.context = context;
    }

    /**
     * The worker's id.
     *
     * @return the id
     */
    public UUID workerId() {
        return workerId;
    }

    /**
     * The specification's id.
     *
     * @return the id
     */
    public String specificationId() {
        return specificationId;
    }

    /**
     * The worker's execution context.
     *
     * @return the context
     */
    public ExecutionContext context() {
        return context;
    }
}
