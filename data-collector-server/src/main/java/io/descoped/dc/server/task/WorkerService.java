package io.descoped.dc.server.task;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.api.util.CommonUtils;
import io.descoped.dc.application.health.HealthResourceFactory;
import io.descoped.dc.application.metrics.MetricsResourceFactory;
import io.descoped.dc.application.spi.Service;
import io.descoped.dc.application.ssl.BusinessSSLResourceSupplier;
import io.descoped.dc.core.executor.Worker;
import io.descoped.dc.core.executor.WorkerObservable;
import io.descoped.dc.core.executor.WorkerObserver;
import io.descoped.dc.core.executor.WorkerStatus;
import io.descoped.dc.core.health.HealthWorkerHistoryResource;
import io.descoped.dc.core.health.HealthWorkerMonitor;
import io.descoped.dc.core.health.HealthWorkerResource;
import io.descoped.dc.server.content.ContentStoreComponent;
import io.descoped.dc.server.ssl.BusinessSSLResourceComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Runs specifications as workers, one at a time for each specification id.
 */
public class WorkerService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerService.class);

    // how long stop() waits for the workers it cancels
    static final String STOP_TIMEOUT = "data.collector.worker.stop.timeoutInSeconds";

    private final DynamicConfiguration configuration;
    private final Duration stopTimeout;
    private final MetricsResourceFactory metricsResourceFactory;
    private final HealthResourceFactory healthResourceFactory;
    private final WorkManager workManager = new WorkManager();
    private final BusinessSSLResourceComponent businessSSLResourceComponent;
    private final boolean printExecutionPlan;
    private final WorkerObserver workerObserver;
    private final Consumer<WorkerLifecycleCallback> workerLifecycleCallback;
    private final ContentStore contentStore;

    /**
     * Creates the service; the execution plan is printed when {@code data.collector.print-execution-plan} is set.
     *
     * @param configuration the configuration
     * @param metricsResourceFactory       the metrics resources
     * @param healthResourceFactory        the health resources
     * @param businessSSLResourceComponent the business certificate resource
     * @param contentStoreComponent        the content store
     */
    public WorkerService(DynamicConfiguration configuration,
                         MetricsResourceFactory metricsResourceFactory,
                         HealthResourceFactory healthResourceFactory,
                         BusinessSSLResourceComponent businessSSLResourceComponent,
                         ContentStoreComponent contentStoreComponent) {
        this(configuration, metricsResourceFactory, healthResourceFactory, businessSSLResourceComponent, contentStoreComponent,
                Boolean.parseBoolean(configuration.find("data.collector.print-execution-plan").orElse(null)), null);
    }

    /**
     * Creates the service.
     *
     * @param configuration                the configuration
     * @param metricsResourceFactory       the metrics resources
     * @param healthResourceFactory        the health resources
     * @param businessSSLResourceComponent the business certificate resource
     * @param contentStoreComponent        the content store
     * @param printExecutionPlan           whether each specification's execution plan is printed
     * @param workerLifecycleCallback      called around each worker's start and finish, or {@code null}
     */
    public WorkerService(DynamicConfiguration configuration,
                         MetricsResourceFactory metricsResourceFactory,
                         HealthResourceFactory healthResourceFactory,
                         BusinessSSLResourceComponent businessSSLResourceComponent,
                         ContentStoreComponent contentStoreComponent,
                         boolean printExecutionPlan,
                         Consumer<WorkerLifecycleCallback> workerLifecycleCallback) {
        this.configuration = configuration;
        this.stopTimeout = stopTimeout(configuration);
        this.metricsResourceFactory = metricsResourceFactory;
        this.healthResourceFactory = healthResourceFactory;
        this.businessSSLResourceComponent = businessSSLResourceComponent;
        this.printExecutionPlan = printExecutionPlan;
        this.workerLifecycleCallback = workerLifecycleCallback;
        this.workerObserver = new WorkerObserver(this::onWorkerStart, this::onWorkerFinish);
        this.contentStore = contentStoreComponent.getDelegate();
    }

    void onWorkerStart(WorkerObservable observable) {
        Optional<Consumer<WorkerLifecycleCallback>> workerLifecycleConsumer = Optional.ofNullable(this.workerLifecycleCallback);
        AtomicReference<WorkerStatus> workerStatus = new AtomicReference<>(WorkerStatus.RUNNING);

        workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_START_BEFORE_TRY_LOCK, workManager, observable, workerStatus.get())));
        workManager.lock(observable.specificationId());
        workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_START_AFTER_TRY_LOCK, workManager, observable, workerStatus.get())));
        try {
            LOG.info("Start worker: {}", observable.workerId());
            HealthWorkerResource healthWorkerResource = healthResourceFactory.createAndAdddHealthResource(observable.workerId(), HealthWorkerResource.class);
            observable.context().services().register(HealthWorkerMonitor.class, healthWorkerResource.getMonitor());
            workerStatus.set(healthWorkerResource.getMonitor().status());
        } finally {
            workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_START_BEFORE_UNLOCK, workManager, observable, workerStatus.get())));
            workManager.unlock(observable.specificationId());
            workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_START_AFTER_UNLOCK, workManager, observable, workerStatus.get())));
        }
    }

    void onWorkerFinish(WorkerObservable observable, WorkerStatus status) {
        Optional<Consumer<WorkerLifecycleCallback>> workerLifecycleConsumer = Optional.ofNullable(this.workerLifecycleCallback);

        workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_FINISH_BEFORE_TRY_LOCK, workManager, observable, status)));
        workManager.lock(observable.specificationId());
        workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_FINISH_AFTER_TRY_LOCK, workManager, observable, status)));
        try {
            if (status == WorkerStatus.COMPLETED) {
                LOG.info("Completed worker: [{}] {}", status, observable.workerId());
            } else {
                LOG.error("Completed worker: [{}] {}", status, observable.workerId());
            }
            workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_FINISH_BEFORE_REMOVE_WORKER, workManager, observable, status)));
            workManager.remove(observable.workerId());
            workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_FINISH_AFTER_REMOVE_WORKER, workManager, observable, status)));

            HealthWorkerResource healthWorkerResource = healthResourceFactory.getHealthResource(observable.workerId());
            healthResourceFactory.removeHealthResource(observable.workerId());
            healthResourceFactory.getHealthResource(HealthWorkerHistoryResource.class).add(healthWorkerResource);
        } finally {
            workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_FINISH_BEFORE_UNLOCK, workManager, observable, status)));
            workManager.unlock(observable.specificationId());
            workerLifecycleConsumer.ifPresent(callback -> callback.accept(new WorkerLifecycleCallback(WorkerLifecycleCallback.Kind.ON_FINISH_AFTER_UNLOCK, workManager, observable, status)));
        }
    }

    /**
     * Starts a worker for a specification, unless one runs for its id.
     *
     * @param specificationBuilder the specification
     * @return the worker's id, or {@code null} if the task was rejected
     */
    public String createOrRejectTask(SpecificationBuilder specificationBuilder) {
        if ("".equals(specificationBuilder.getId())) {
            LOG.warn("The specification id is empty!");
            return null;
        }

        workManager.lock(specificationBuilder.getId());
        try {
            if (workManager.isRunning(specificationBuilder.getId())) {
                LOG.warn("The specification '{}' is already running!", specificationBuilder.getId());
                return null;
            }

            Worker.WorkerBuilder workerBuilder = Worker.newBuilder()
                    .configuration(configuration.asMap())
                    .workerObserver(workerObserver)
                    .specification(specificationBuilder)
                    .contentStore(contentStore)
                    .keepContentStoreOpenOnWorkerCompletion(false);

            if (printExecutionPlan) {
                workerBuilder
                        .printConfiguration()
                        .printExecutionPlan();
            }

            if (businessSSLResourceComponent == null || businessSSLResourceComponent.getDelegate() == null) {
                String configuredCertBundlesPath = configuration.find("data.collector.certs.directory").orElse(null);
                Path certBundlesPath = configuredCertBundlesPath == null ? CommonUtils.currentPath() : Paths.get(configuredCertBundlesPath);
                workerBuilder.buildCertificateFactory(certBundlesPath);
            } else {
                BusinessSSLResourceSupplier businessSSLBundleSupplier = businessSSLResourceComponent.getDelegate();
                workerBuilder.useBusinessSSLResourceSupplier(businessSSLBundleSupplier.get());
            }

            return workManager.run(workerBuilder).workerId.toString();
        } finally {
            workManager.unlock(specificationBuilder.getId());
        }
    }

    /**
     * The running workers.
     *
     * @return the tasks
     */
    public List<WorkManager.Task> list() {
        return workManager.list();
    }

    /**
     * Cancels a worker.
     *
     * @param workerId the worker's id
     * @return true if it was found
     */
    public boolean cancelTask(String workerId) {
        // the lock is held only while the worker's termination is set, so onWorkerFinish waits no longer than that
        WorkManager.JobId jobId = workManager.get(UUID.fromString(workerId));
        if (jobId == null) {
            LOG.warn("Worker '{}' NOT found! Maybe it was already completed.", workerId);
            return false;
        }
        workManager.lock(jobId.specificationId);
        try {
            return workManager.cancel(UUID.fromString(workerId));
        } finally {
            workManager.unlock(jobId.specificationId);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void start() {
        // nop
    }

    @Override
    public void stop() {
        workManager.cancel(stopTimeout);
    }

    // read at start-up, so a value that is not a number of seconds fails then, not when the server stops (#45)
    static Duration stopTimeout(DynamicConfiguration configuration) {
        String value = configuration.find(STOP_TIMEOUT).orElse("30");
        long seconds;
        try {
            seconds = Long.parseLong(value.trim());
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException(STOP_TIMEOUT + " is not a whole number of seconds");
        }
        if (seconds < 0) {
            throw new IllegalArgumentException(STOP_TIMEOUT + " is negative");
        }
        return Duration.ofSeconds(seconds);
    }
}
