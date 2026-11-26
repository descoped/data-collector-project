package io.descoped.dc.api;

import io.descoped.dc.api.context.ExecutionContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The state of a fetched page: the expected positions, the next page's positions, the parallel executions of
 * its entries, and whether the stream ended or failed.
 */
public class PageContext {

    private final List<String> expectedPositions;
    private final List<String> nextPositionVariableNames;
    private final Map<String, String> nextPositionMap; // variableName, positionValue
    private final List<Entry> parallelFutures = new ArrayList<>();
    private final CompletionInfo completionInfo = new CompletionInfo();
    private final AtomicBoolean endOfStream = new AtomicBoolean(false);
    private final AtomicReference<Throwable> failureCause = new AtomicReference<>();

    private PageContext(List<String> expectedPositions, List<String> nextPositionVariableNames, Map<String, String> nextPositionMap) {
        this.expectedPositions = expectedPositions;
        this.nextPositionVariableNames = nextPositionVariableNames;
        this.nextPositionMap = nextPositionMap;
    }

    /**
     * An empty page context that marks the end of the stream.
     *
     * @return the page context
     */
    public static PageContext createEndOfStream() {
        PageContext pageContext = new PageContext(new LinkedList<>(), new LinkedList<>(), new LinkedHashMap<>());
        pageContext.endOfStream.set(true);
        return pageContext;
    }

    /**
     * The positions the page is expected to give.
     *
     * @return the positions
     */
    public List<String> expectedPositions() {
        return expectedPositions;
    }

    /**
     * The names of the variables of the next page's position.
     *
     * @return the names
     */
    public List<String> nextPositionVariableNames() {
        return nextPositionVariableNames;
    }

    /**
     * The next page's position values, by variable name.
     *
     * @return the values
     */
    public Map<String, String> nextPositionMap() {
        return nextPositionMap;
    }

    /**
     * Adds the future of an entry's parallel execution.
     *
     * @param future the future
     */
    public void addFuture(CompletableFuture<ExecutionContext> future) {
        parallelFutures.add(new Entry(future));
    }

    /**
     * The parallel executions of the page's entries.
     *
     * @return the entries
     */
    public List<Entry> parallelPageEntries() {
        return parallelFutures;
    }

    /**
     * The futures of the parallel executions of the page's entries.
     *
     * @return the futures
     */
    public List<CompletableFuture<ExecutionContext>> parallelFutures() {
        return parallelFutures.stream().map(Entry::future).toList();
    }

    /**
     * The counts of queued and completed entries.
     *
     * @return the counts
     */
    public CompletionInfo completionInfo() {
        return completionInfo;
    }

    /**
     * Adds one to the count of queued entries.
     */
    public void incrementQueueCount() {
        completionInfo.queueCount.incrementAndGet();
    }

    /**
     * Adds one to the count of completed entries.
     */
    public void incrementCompletionCount() {
        completionInfo.completedCount.incrementAndGet();
    }

    /**
     * Whether the page marks the end of the stream.
     *
     * @return {@code true} at the end of the stream
     */
    public boolean isEndOfStream() {
        return endOfStream.get();
    }

    /**
     * Sets whether the page marks the end of the stream.
     *
     * @param endOfStream {@code true} at the end of the stream
     */
    public void setEndOfStream(boolean endOfStream) {
        this.endOfStream.set(endOfStream);
    }

    /**
     * The holder of the failure's cause.
     *
     * @return the holder
     */
    public AtomicReference<Throwable> failureCause() {
        return failureCause;
    }

    /**
     * Whether the page failed.
     *
     * @return {@code true} if a failure cause is set
     */
    public boolean isFailure() {
        return failureCause.get() != null;
    }

    /**
     * Sets the failure's cause.
     *
     * @param cause the cause
     * @return this page context
     */
    public PageContext failure(Throwable cause) {
        failureCause.set(cause);
        return this;
    }

    /**
     * The failure's cause.
     *
     * @return the cause, or {@code null} if the page did not fail
     */
    public Throwable getFailureCause() {
        return failureCause.get();
    }

    /**
     * The parallel execution of a page entry.
     */
    public static class Entry {
        private final CompletableFuture<ExecutionContext> future;

        /**
         * Creates the entry.
         *
         * @param future the execution's future
         */
        public Entry(CompletableFuture<ExecutionContext> future) {
            this.future = future;
        }

        /**
         * The execution's future.
         *
         * @return the future
         */
        public CompletableFuture<ExecutionContext> future() {
            return future;
        }
    }

    /**
     * The counts of a page's queued and completed entries.
     */
    public static class CompletionInfo {
        final AtomicLong queueCount = new AtomicLong(0);
        final AtomicLong completedCount = new AtomicLong(0);

        /**
         * Creates an empty instance.
         */
        public CompletionInfo() {
            // the state is in the fields
        }

        /**
         * The count of queued entries.
         *
         * @return the count
         */
        public AtomicLong queueCount() {
            return queueCount;
        }

        /**
         * The count of completed entries.
         *
         * @return the count
         */
        public AtomicLong completedCount() {
            return completedCount;
        }
    }

    /**
     * A builder of a {@link PageContext}.
     */
    public static class Builder {
        private List<String> expectedPositions;
        private final Map<String, String> nextPositionMap = new LinkedHashMap<>();
        private List<String> nextPositionVariableNames;

        /**
         * Creates an empty builder.
         */
        public Builder() {
            // the state is in the fields
        }

        /**
         * Sets the expected positions.
         *
         * @param expectedPositions the positions
         * @return this builder
         */
        public Builder expectedPositions(List<String> expectedPositions) {
            this.expectedPositions = expectedPositions;
            return this;
        }

        /**
         * Sets the names of the variables of the next page's position.
         *
         * @param nextPositionVariableNames the names
         * @return this builder
         */
        public Builder addNextPositionVariableNames(List<String> nextPositionVariableNames) {
            this.nextPositionVariableNames = nextPositionVariableNames;
            return this;
        }

        /**
         * The names of the variables of the next page's position set so far.
         *
         * @return the names, or {@code null} if none are set
         */
        public List<String> nextPositionVariableNames() {
            return nextPositionVariableNames;
        }

        /**
         * Sets a value of the next page's position.
         *
         * @param variableName the variable name
         * @param position     the position value
         * @return this builder
         */
        public Builder addNextPosition(String variableName, String position) {
            nextPositionMap.put(variableName, position);
            return this;
        }

        /**
         * Builds the page context.
         *
         * @return the page context
         */
        public PageContext build() {
            return new PageContext(expectedPositions, nextPositionVariableNames, nextPositionMap);
        }
    }
}
