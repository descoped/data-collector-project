package io.descoped.dc.server.recovery;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.util.CommonUtils;
import io.descoped.dc.application.spi.Service;
import io.descoped.dc.server.content.ContentStoreComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static io.descoped.dc.server.db.SequenceDbHelper.getSequenceDatabaseLocation;

/*
 * Run the integrity checker before running recovery.
 */

/**
 * Runs recoveries: copies a topic to another, one message per position, from an integrity check's index.
 */
public class RecoveryService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(RecoveryService.class);

    private final DynamicConfiguration configuration;
    private final ContentStoreComponent contentStoreComponent;
    private final RecoveryContentStoreComponent recoveryContentStoreComponent;
    final Map<String, CompletableFuture<RecoveryWorker>> jobFutures = new ConcurrentHashMap<>();
    final Map<String, RecoveryWorker> jobs = new ConcurrentHashMap<>();
    // the source topics whose recovery is starting or running, claimed before it starts (#67)
    private final Set<String> active = ConcurrentHashMap.newKeySet();
    // the source topics asked to stop while their recovery was starting: it stops itself when it registers
    private final Set<String> cancelWhenRegistered = ConcurrentHashMap.newKeySet();

    /**
     * Creates the service.
     *
     * @param configuration the configuration
     * @param contentStoreComponent         the content store copied from
     * @param recoveryContentStoreComponent the content store copied to
     */
    public RecoveryService(DynamicConfiguration configuration, ContentStoreComponent contentStoreComponent, RecoveryContentStoreComponent recoveryContentStoreComponent) {
        this.configuration = configuration;
        this.contentStoreComponent = contentStoreComponent;
        this.recoveryContentStoreComponent = recoveryContentStoreComponent;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void start() {
        // a recovery starts on request (createRecoveryWorker)
    }

    @Override
    public void stop() {
        for (Map.Entry<String, RecoveryWorker> entry : jobs.entrySet()) {
            entry.getValue().terminate();
        }
    }

    List<Path> getSequenceDatabaseList() {
        Path dbLocation = getSequenceDatabaseLocation(configuration);
        try {
            try (Stream<Path> paths = Files.list(dbLocation)) {
                return paths.filter(path -> path.toFile().isDirectory()).toList();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // whether a recovery of the topic is starting or running
    boolean isActive(String fromTopic) {
        return active.contains(fromTopic);
    }

    // false when a recovery of the topic is starting or running
    boolean createRecoveryWorker(String fromTopic, String toTopic) {
        Path dbLocation = getSequenceDatabaseLocation(configuration);
        LOG.trace("Database path: {}", dbLocation);
        if (!dbLocation.toFile().exists()) {
            throw new IllegalStateException("Lmdb Location does not exist: " + dbLocation.toString());
        }
        if (!active.add(fromTopic)) {
            return false;
        }
        boolean started = false;
        try {
            jobFutures.put(fromTopic, startRecovery(fromTopic, toTopic));
            started = true;
        } finally {
            if (!started) {
                active.remove(fromTopic);
            }
        }
        return true;
    }

    // asks the topic's recovery to stop, also one that is still starting
    void cancel(String fromTopic) {
        RecoveryWorker worker = jobs.get(fromTopic);
        if (worker != null && worker.monitor().running.get()) {
            worker.terminate();
        } else if (active.contains(fromTopic)) {
            // not registered yet: it stops itself when it registers; checked again in case it registered in between,
            // and terminating twice is harmless
            cancelWhenRegistered.add(fromTopic);
            RecoveryWorker registered = jobs.get(fromTopic);
            if (registered != null && registered != worker) {
                registered.terminate();
            }
        }
    }

    private CompletableFuture<RecoveryWorker> startRecovery(String fromTopic, String toTopic) {
        return CompletableFuture.supplyAsync(() -> {
            RecoveryWorker recoveryWorker = new RecoveryWorker(configuration, contentStoreComponent, recoveryContentStoreComponent);
            jobs.put(fromTopic, recoveryWorker);
            if (cancelWhenRegistered.remove(fromTopic)) {
                recoveryWorker.terminate();
            }
            recoveryWorker.recover(fromTopic, toTopic);
            LOG.trace("Completed Recovery!");
            return recoveryWorker;
        }).whenComplete((recoveryWorker, throwable) -> {
            cancelWhenRegistered.remove(fromTopic);
            active.remove(fromTopic);
        }).exceptionally(throwable -> {
            LOG.error("Ended exceptionally with error: {}", CommonUtils.captureStackTrace(throwable));
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (throwable instanceof Error error) {
                throw error;
            } else {
                throw new RuntimeException(throwable);
            }
        });
    }
}
