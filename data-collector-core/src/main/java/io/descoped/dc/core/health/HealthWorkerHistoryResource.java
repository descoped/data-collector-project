package io.descoped.dc.core.health;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;
import io.descoped.dc.api.util.JsonParser;
import io.descoped.dc.core.executor.WorkerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The health resource {@code worker-history}: the resources of completed workers, newest first.
 */
@HealthRenderPriority(priority = 15)
public class HealthWorkerHistoryResource implements HealthResource {

    static final int MAX_HISTORY_CAPACITY = 25;

    final Lock lock = new ReentrantLock();
    final CopyOnWriteArrayList<ObjectNode> historyList = new CopyOnWriteArrayList<>();

    /**
     * Creates the resource.
     */
    public HealthWorkerHistoryResource() {
        // the state is in the fields
    }

    @Override
    public Optional<Boolean> isUp() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "worker-history";
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return true;
    }

    @Override
    public Object resource() {
        List<ObjectNode> reversedList = new ArrayList<>(historyList);
        Collections.reverse(reversedList);
        return reversedList;
    }

    /**
     * Adds a snapshot of the worker's resource to the history. The history keeps the latest 25: when it is full, the
     * oldest entry is removed before the new one is added.
     *
     * @param workerResource the worker's health resource
     * @throws WorkerException when the thread is interrupted while waiting for the lock
     */
    public void add(HealthWorkerResource workerResource) {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorkerException(e);
        }
        try {
            JsonParser jsonParser = JsonParser.createJsonParser();
            ObjectNode convertedNode = jsonParser.mapper().convertValue(workerResource.resource(), ObjectNode.class);
            // the latest MAX_HISTORY_CAPACITY workers: the oldest goes first
            while (historyList.size() >= MAX_HISTORY_CAPACITY) {
                historyList.remove(0);
            }
            historyList.add(convertedNode);

        } finally {
            lock.unlock();
        }
    }
}
