package io.descoped.dc.core.handler;

import io.descoped.dc.api.PositionObserver;
import io.descoped.dc.api.Termination;
import io.descoped.dc.core.health.HealthWorkerMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Decides when to prefetch the next page: it counts the expected and the completed positions, and runs the prefetch
 * when the number of pending positions falls below the threshold and no prefetch is pending.
 */
public class PrefetchAlgorithm {

    static final Logger LOG = LoggerFactory.getLogger(PrefetchAlgorithm.class);

    final int prefetchThreshold;
    final Runnable prefetchRunnable;
    final Termination termination;
    final HealthWorkerMonitor monitor;

    final AtomicLong expectedPositionCounter = new AtomicLong();
    final AtomicLong positionCompletedCounter = new AtomicLong();
    final AtomicLong pendingPrefetches = new AtomicLong(1);

    /**
     * Creates the algorithm; it starts with one prefetch pending, the first page.
     *
     * @param prefetchThreshold the number of pending positions below which the next page is prefetched
     * @param prefetchRunnable  the prefetch of the next page
     * @param termination       the worker's termination signal
     * @param monitor           the health monitor to update with the counts, or {@code null}
     */
    public PrefetchAlgorithm(int prefetchThreshold, Runnable prefetchRunnable, Termination termination, HealthWorkerMonitor monitor) {
        this.prefetchThreshold = prefetchThreshold;
        this.prefetchRunnable = prefetchRunnable;
        this.termination = termination;
        this.monitor = monitor;
    }

    /**
     * A position observer that reports expected and completed positions to this algorithm.
     *
     * @return a new position observer
     */
    public PositionObserver getPositionObserver() {
        return new PositionObserver(expectedConsumerFunction(), completedConsumerFunction());
    }

    private Consumer<Integer> expectedConsumerFunction() {
        return expectedCount -> {
            long total = expectedPositionCounter.addAndGet(expectedCount);
            if (LOG.isTraceEnabled()) {
                LOG.trace("expected observed: added={}, total={}", expectedCount, total);
            }

            if (monitor != null) {
                monitor.request().updateTotalExpectedCount(expectedCount);
            }

            long countAfterDecrement = pendingPrefetches.decrementAndGet();
            if (countAfterDecrement < 0) {
                throw new IllegalStateException("count-after-decrement < 0");
            }

            prefetchIfBelowThreshold(expectedPositionCounter.get() - positionCompletedCounter.get());
        };
    }

    private Consumer<Integer> completedConsumerFunction() {
        return completedCount -> {
            long totalCompletedCount = positionCompletedCounter.addAndGet(completedCount);
            if (LOG.isTraceEnabled()) {
                LOG.trace("completed observed: added={}, total={}", completedCount, totalCompletedCount);
            }

            if (monitor != null) {
                monitor.request().updateTotalCompletedCount(completedCount);
            }

            if (pendingPrefetches.get() > 0) {
                return; // threshold will be checked when expected counter is increased
            }

            prefetchIfBelowThreshold(expectedPositionCounter.get() - totalCompletedCount);
        };
    }

    private void prefetchIfBelowThreshold(long pendingPositions) {
        if (pendingPositions < prefetchThreshold) {

            if (!pendingPrefetches.compareAndSet(0, 1)) {
                return; // a concurrent thread won the race to start a pre-fetch
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("Pre-fetching next-page...");
            }

            prefetchRunnable.run();
        }
    }
}
