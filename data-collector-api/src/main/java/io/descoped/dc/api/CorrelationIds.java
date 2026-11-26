package io.descoped.dc.api;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.ids.TimeOrderedIds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The correlation ids of an execution, kept in the execution context's state, in the order they were added.
 */
public class CorrelationIds {

    final Deque<UUID> correlationIds = new ConcurrentLinkedDeque<>();
    final Lock lock = new ReentrantLock();

    private CorrelationIds() {
    }

    /**
     * The correlation ids in the context's state, created and put there if absent.
     *
     * @param context the context
     * @return the correlation ids
     */
    public static CorrelationIds of(ExecutionContext context) {
        CorrelationIds cid = context.state(CorrelationIds.class);
        if (cid == null) {
            cid = new CorrelationIds();
            context.state(CorrelationIds.class, cid);
        }
        return cid;
    }

    /**
     * Creates empty correlation ids and puts them in the context's state, replacing any there.
     *
     * @param context the context
     * @return the correlation ids
     */
    public static CorrelationIds create(ExecutionContext context) {
        CorrelationIds cid = new CorrelationIds();
        context.state(CorrelationIds.class, cid);
        return cid;
    }

    /**
     * Adds a new time-ordered id.
     *
     * @return the id
     */
    public UUID add() {
        UUID uuid = TimeOrderedIds.generate();
        correlationIds.add(uuid);
        return uuid;
    }

    /**
     * Adds the last id of other correlation ids.
     *
     * @param correlationIds the other correlation ids
     */
    public void tail(CorrelationIds correlationIds) {
        this.correlationIds.add(correlationIds.last());
    }


    /**
     * The first id.
     *
     * @return the id, or {@code null} if there is none
     */
    public UUID first() {
        return correlationIds.peekFirst();
    }

    /**
     * The last id.
     *
     * @return the id, or {@code null} if there is none
     */
    public UUID last() {
        return correlationIds.peekLast();
    }

    /**
     * A copy of the ids.
     *
     * @return the ids, or an empty list if the lock is not acquired within one second
     */
    public List<UUID> get() {
        try {
            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    return new ArrayList<>(correlationIds);
                } finally {
                    lock.unlock();
                }
            }
            return Collections.emptyList();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * A new execution context with these correlation ids in its state.
     *
     * @return the context
     */
    public ExecutionContext context() {
        return ExecutionContext.empty().state(CorrelationIds.class, this);
    }
}
