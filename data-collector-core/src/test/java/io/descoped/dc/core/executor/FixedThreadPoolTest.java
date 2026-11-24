package io.descoped.dc.core.executor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedThreadPoolTest {

    @Test
    void thatPoolInfoHasItsSizes() {
        FixedThreadPool threadPool = FixedThreadPool.newInstance(2);
        try {
            assertEquals(2, threadPool.asThreadPoolInfo().get("core-pool-size"));
        } finally {
            threadPool.shutdownAndAwaitTermination();
        }
    }

    // an interrupted wait cancels the running tasks and keeps the interrupt flag
    @Test
    void thatInterruptedShutdownCancelsTasksAndRestoresTheInterrupt() throws InterruptedException {
        FixedThreadPool threadPool = FixedThreadPool.newInstance(1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        CountDownLatch never = new CountDownLatch(1);
        threadPool.getExecutor().execute(() -> {
            started.countDown();
            try {
                never.await();
            } catch (InterruptedException e) {
                cancelled.countDown();
            }
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));

        Thread.currentThread().interrupt();
        threadPool.shutdownAndAwaitTermination();

        assertTrue(Thread.interrupted());
        assertTrue(cancelled.await(5, TimeUnit.SECONDS));
    }
}
