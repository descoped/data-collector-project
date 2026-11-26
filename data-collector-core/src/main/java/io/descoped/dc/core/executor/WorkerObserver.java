package io.descoped.dc.core.executor;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Callbacks for a worker's start and finish.
 */
public class WorkerObserver {

    private final Consumer<WorkerObservable> onStartCallback;
    private final BiConsumer<WorkerObservable, WorkerStatus> onFinishCallback;

    /**
     * Creates the observer.
     *
     * @param onStartCallback  called when the worker starts
     * @param onFinishCallback called when the worker finishes, with its status
     */
    public WorkerObserver(Consumer<WorkerObservable> onStartCallback, BiConsumer<WorkerObservable, WorkerStatus> onFinishCallback) {
        this.onStartCallback = onStartCallback;
        this.onFinishCallback = onFinishCallback;
    }

    /**
     * Calls the start callback.
     *
     * @param observable the worker
     */
    public void start(WorkerObservable observable) {
        onStartCallback.accept(observable);
    }

    /**
     * Calls the finish callback.
     *
     * @param observable the worker
     * @param outcome    the worker's status
     */
    public void finish(WorkerObservable observable, WorkerStatus outcome) {
        onFinishCallback.accept(observable, outcome);
    }
}
