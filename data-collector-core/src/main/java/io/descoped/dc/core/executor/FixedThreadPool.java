package io.descoped.dc.core.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FixedThreadPool {

    private static final Logger LOG = LoggerFactory.getLogger(FixedThreadPool.class);

    private final ExecutorService executorService;

    private FixedThreadPool(int numberOfThreads) {
        this.executorService = createFixedThreadPool(numberOfThreads);
    }

    public static FixedThreadPool newInstance() {
        return new FixedThreadPool(-1);
    }

    public static FixedThreadPool newInstance(int numberOfThreads) {
        return new FixedThreadPool(numberOfThreads);
    }

    static ExecutorService createFixedThreadPool(int numberOfThreads) {
        int configuredThreadCount = numberOfThreads == -1 ? getNumberOfThreads() : numberOfThreads;
        LOG.info("Number of worker threads set to: {}", configuredThreadCount);
        return Executors.newFixedThreadPool(configuredThreadCount);
    }

    public static int getNumberOfThreads() {
        return Runtime.getRuntime().availableProcessors() + 2;
    }

    public ExecutorService getExecutor() {
        return executorService;
    }

    public Map<String, Object> asThreadPoolInfo() {
        Map<String, Object> threadPoolInfoMap = new LinkedHashMap<>();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        threadPoolInfoMap.put("core-pool-size", threadPoolExecutor.getCorePoolSize());
        threadPoolInfoMap.put("maximum-pool-size", threadPoolExecutor.getMaximumPoolSize());
        return threadPoolInfoMap;
    }

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
