package io.descoped.dc.core.executor;

/**
 * The status of a worker.
 */
public enum WorkerStatus {
    /**
     * The worker runs.
     */
    RUNNING,
    /**
     * The worker completed without failure.
     */
    COMPLETED,
    /**
     * The worker failed.
     */
    FAILED,
    /**
     * The worker was terminated.
     */
    CANCELED;
}
