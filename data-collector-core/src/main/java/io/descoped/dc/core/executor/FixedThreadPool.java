package io.descoped.dc.core.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A fixed size thread pool for a worker.
 */
public class FixedThreadPool {

    private static final Logger LOG = LoggerFactory.getLogger(FixedThreadPool.class);

    private final ExecutorService executorService;

    private FixedThreadPool(int numberOfThreads) {
        this.executorService = createFixedThreadPool(numberOfThreads);
    }

    /**
     * Creates a pool of {@link #getNumberOfThreads()} threads.
     *
     * @return the pool
     */
    public static FixedThreadPool newInstance() {
        return new FixedThreadPool(-1);
    }

    /**
     * Creates a pool of the given size.
     *
     * @param numberOfThreads the number of threads, or {@code -1} for {@link #getNumberOfThreads()}
     * @return the pool
     */
    public static FixedThreadPool newInstance(int numberOfThreads) {
        return new FixedThreadPool(numberOfThreads);
    }

    static ExecutorService createFixedThreadPool(int numberOfThreads) {
        int configuredThreadCount = numberOfThreads == -1 ? getNumberOfThreads() : numberOfThreads;
        LOG.info("Number of worker threads set to: {}", configuredThreadCount);
        return Executors.newFixedThreadPool(configuredThreadCount);
    }

    /**
     * The default number of threads: the available processors plus two.
     *
     * @return the number of threads
     */
    public static int getNumberOfThreads() {
        return Runtime.getRuntime().availableProcessors() + 2;
    }

    /**
     * The pool's executor service.
     *
     * @return the executor service
     */
    public ExecutorService getExecutor() {
        return executorService;
    }

    /**
     * The pool's core and maximum pool size, for the health output.
     *
     * @return the pool sizes by name
     */
    public Map<String, Object> asThreadPoolInfo() {
        Map<String, Object> threadPoolInfoMap = new LinkedHashMap<>();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        threadPoolInfoMap.put("core-pool-size", threadPoolExecutor.getCorePoolSize());
        threadPoolInfoMap.put("maximum-pool-size", threadPoolExecutor.getMaximumPoolSize());
        return threadPoolInfoMap;
    }

    /**
     * Shuts the pool down and waits up to 5 seconds for its tasks, then cancels them and waits up to 60 seconds more.
     * When the calling thread is interrupted, the tasks are cancelled and the interrupt status is kept.
     */
    public void shutdownAndAwaitTermination() {
        LOG.debug("Shutdown thread pool..");
        executorService.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    LOG.error("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        LOG.info("Thread pool is terminated!");
    }
}
