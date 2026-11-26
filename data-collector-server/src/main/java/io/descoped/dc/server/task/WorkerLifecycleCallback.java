package io.descoped.dc.server.task;

import io.descoped.dc.core.executor.WorkerObservable;
import io.descoped.dc.core.executor.WorkerStatus;

/**
 * This class is used for debugging and profiling purposes only.
 * Should be removed when FAILED tasks that are NOT removed from WorkManager is fixed
 */
public class WorkerLifecycleCallback {

    /**
     * When the callback is called: at a worker's start or finish, around taking and releasing the specification's lock.
     */
    public enum Kind {
        /**
         * At start, before the lock is taken.
         */
        ON_START_BEFORE_TRY_LOCK,
        /**
         * At start, after the lock is taken.
         */
        ON_START_AFTER_TRY_LOCK,
        /**
         * At start, before the lock is released.
         */
        ON_START_BEFORE_UNLOCK,
        /**
         * At start, after the lock is released.
         */
        ON_START_AFTER_UNLOCK,
        /**
         * At finish, before the lock is taken.
         */
        ON_FINISH_BEFORE_TRY_LOCK,
        /**
         * At finish, after the lock is taken.
         */
        ON_FINISH_AFTER_TRY_LOCK,
        /**
         * At finish, before the worker is removed.
         */
        ON_FINISH_BEFORE_REMOVE_WORKER,
        /**
         * At finish, after the worker is removed.
         */
        ON_FINISH_AFTER_REMOVE_WORKER,
        /**
         * At finish, before the lock is released.
         */
        ON_FINISH_BEFORE_UNLOCK,
        /**
         * At finish, after the lock is released.
         */
        ON_FINISH_AFTER_UNLOCK;
    }

    final Kind kind;
    final WorkManager workManager;
    final WorkerObservable workerObservable;
    final WorkerStatus workerStatus;

    /**
     * Creates the callback's argument.
     *
     * @param kind             when it is called
     * @param workManager      the work manager
     * @param workerObservable the worker
     * @param workerStatus     the worker's status
     */
    public WorkerLifecycleCallback(Kind kind, WorkManager workManager, WorkerObservable workerObservable, WorkerStatus workerStatus) {
        this.kind = kind;
        this.workManager = workManager;
        this.workerObservable = workerObservable;
        this.workerStatus = workerStatus;
    }
}
