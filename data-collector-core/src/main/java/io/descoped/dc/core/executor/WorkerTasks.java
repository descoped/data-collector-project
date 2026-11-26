package io.descoped.dc.core.executor;

import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The tasks a worker started on its thread pool, such as the entries of a {@code Parallel} node. The worker waits for
 * them when its start node returns, so a task that fails fails the worker, also outside a {@code Paginate} (which
 * waits for its pages' tasks itself). A task is forgotten once it is done; only the first failure is kept.
 */
public final class WorkerTasks {

    private final Set<CompletableFuture<?>> running = ConcurrentHashMap.newKeySet();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    /**
     * Creates the tasks of a worker.
     */
    public WorkerTasks() {
        // the tasks are added as they start
    }

    /**
     * Adds a task.
     *
     * @param task the task
     */
    public void add(CompletableFuture<?> task) {
        running.add(task);
        task.whenComplete((result, throwable) -> {
            running.remove(task);
            if (throwable != null) {
                failure.compareAndSet(null, unwrap(throwable));
            }
        });
    }

    /**
     * Waits until every task is done, also the tasks added while it waits, and throws the first failure.
     *
     * @throws RuntimeException the first task's failure, as it was thrown; a checked exception wrapped in a
     *                          {@link CompletionException}
     */
    public void awaitAll() {
        while (!running.isEmpty()) {
            CompletableFuture<?>[] tasks = running.toArray(new CompletableFuture<?>[0]);
            try {
                CompletableFuture.allOf(tasks).join();
            } catch (CompletionException | CancellationException _) {
                // the failure is kept when the task completes
            }
        }
        Throwable first = failure.get();
        if (first instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (first instanceof Error error) {
            throw error;
        }
        if (first != null) {
            throw new CompletionException(first);
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
