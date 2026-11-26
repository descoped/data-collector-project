package io.descoped.dc.core.executor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.ConfigurationMap;
import io.descoped.dc.api.Termination;
import io.descoped.dc.api.TerminationException;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.node.FlowContext;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.api.node.NodeWithId;
import io.descoped.dc.api.node.Paginate;
import io.descoped.dc.api.node.Security;
import io.descoped.dc.api.node.builder.NodeBuilder;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.api.observer.WorkerEvent;
import io.descoped.dc.api.security.BusinessSSLResource;
import io.descoped.dc.api.security.Masking;
import io.descoped.dc.api.services.Services;
import io.descoped.dc.api.ids.TimeOrderedIds;
import io.descoped.dc.api.util.CommonUtils;
import io.descoped.dc.api.util.JsonParser;
import io.descoped.dc.core.content.ObservedContentStore;
import io.descoped.dc.core.handler.ParallelHandler;
import io.descoped.dc.core.health.HealthThreadsResource;
import io.descoped.dc.core.health.HealthWorkerMonitor;
import io.descoped.dc.core.http.ObservedClient;
import io.descoped.dc.core.observer.EngineObservers;
import io.descoped.dc.core.security.CertificateContext;
import io.descoped.dc.core.security.CertificateFactory;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Runs one specification: executes its start node with the worker's execution context, publishes to the content
 * store, keeps the health monitor up to date and tells the observers when it starts and finishes. Made with
 * {@link #newBuilder()}.
 */
public class Worker {

    private static final Logger LOG = LoggerFactory.getLogger(Worker.class);
    private static final String HTTP_CLIENT_TIMEOUT_SECONDS = "data.collector.http.client.timeout.seconds";
    private static final String HTTP_REQUEST_TIMEOUT_SECONDS = "data.collector.http.request.timeout.seconds";
    private static final String CONTENT_STREAM_CONNECTOR = "content.stream.connector";
    private final UUID workerId;
    private final String specificationId;
    private final String name;
    private final List<WorkerObserver> workerObservers;
    private final Node node;
    private final ExecutionContext context;
    private final boolean keepContentStoreOpenOnWorkerCompletion;

    /**
     * Creates a worker with a new id.
     *
     * @param specificationId                        the specification's id
     * @param name                                   the specification's name
     * @param workerObservers                        the observers to tell when the worker starts and finishes; the engine
     *                                               observers are told first when it starts, and last when it finishes
     * @param node                                   the start node
     * @param context                                the execution context
     * @param keepContentStoreOpenOnWorkerCompletion whether the topic is left open when the worker ends
     */
    public Worker(String specificationId, String name, List<WorkerObserver> workerObservers, Node node, ExecutionContext context, boolean keepContentStoreOpenOnWorkerCompletion) {
        this.specificationId = specificationId;
        this.name = name;
        this.workerObservers = withEngineObservers(name, workerObservers);
        this.workerId = TimeOrderedIds.generate();
        this.node = node;
        this.context = context;
        this.keepContentStoreOpenOnWorkerCompletion = keepContentStoreOpenOnWorkerCompletion;
    }

    // the engine observers hear of the worker through a worker observer of their own, the first one
    private static List<WorkerObserver> withEngineObservers(String name, List<WorkerObserver> workerObservers) {
        EngineObservers engineObservers = EngineObservers.instance();
        List<WorkerObserver> observers = new ArrayList<>(workerObservers.size() + 1);
        observers.add(new WorkerObserver(
                observable -> engineObservers.workerStarted(new WorkerEvent(observable.workerId(), observable.specificationId(), name)),
                (observable, status) -> engineObservers.workerFinished(new WorkerEvent(observable.workerId(), observable.specificationId(), name), status.name())));
        observers.addAll(workerObservers);
        return observers;
    }

    /**
     * Creates a worker builder.
     *
     * @return the builder
     */
    public static WorkerBuilder newBuilder() {
        return new WorkerBuilder();
    }

    /**
     * The specification's id.
     *
     * @return the id
     */
    public String getSpecificationId() {
        return specificationId;
    }

    /**
     * The specification's name.
     *
     * @return the name
     */
    public String getSpecificationName() {
        return name;
    }

    /**
     * The worker's id.
     *
     * @return the id
     */
    public UUID getWorkerId() {
        return workerId;
    }

    /**
     * The worker's execution context.
     *
     * @return the context
     */
    public ExecutionContext context() {
        return context;
    }

    /*
     * Aimed at test case scenarios where you want to execute subsequent runs
     */

    // the producers of the worker's topic, and of its page topic when a paginate adds page content
    private void openTopics() {
        String topic = node.configurations().flowContext().topic();
        ContentStore contentStore = context.services().get(ContentStore.class);
        if (topic == null || contentStore == null || contentStore.isClosed()) {
            return;
        }
        contentStore.contentStream().producer(topic);
        AtomicBoolean addsPageContent = new AtomicBoolean();
        node.traverse(0, new LinkedHashSet<>(), new ArrayList<>(), (ancestors, descendant) -> {
            if (descendant instanceof Paginate paginate && paginate.addPageContent()) {
                addsPageContent.set(true);
            }
        });
        if (addsPageContent.get()) {
            contentStore.contentStream().producer(topic + "-pages");
        }
    }

    /**
     * Removes the iteration limit and resets the iteration count of the parallel handler, so that the worker can run
     * again.
     */
    public void resetMaxNumberOfIterations() {
        context.state(ParallelHandler.MAX_NUMBER_OF_ITERATIONS, null);
        if (context.services().contains(ParallelHandler.Counters.class)) {
            context.services().get(ParallelHandler.Counters.class).resetNumberOfIterations();
        }
    }

    /**
     * Runs the worker on the calling thread: tells the observers it starts, executes the start node, waits for the
     * tasks it left running ({@link WorkerTasks}; the first that failed fails the worker), closes the topic
     * (unless the content store is kept open) and shuts the thread pool down. Then it sets the status on the monitor
     * ({@link WorkerStatus#CANCELED} when terminated, {@link WorkerStatus#FAILED} on another failure) and tells the
     * observers, in reverse order, that it finished. A failure is rethrown, a checked exception wrapped in a
     * {@link WorkerException}.
     *
     * @return the output context of the start node
     */
    public ExecutionContext run() {
        AtomicBoolean startWorkerObserverIsFired = new AtomicBoolean(false);
        AtomicBoolean finishWorkerObserverIsFired = new AtomicBoolean(false);
        AtomicBoolean threadPoolIsTerminated = new AtomicBoolean(false);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            if (!workerObservers.isEmpty()) {
                if (LOG.isDebugEnabled()) LOG.debug("[{}] Fire observer onStart", workerId);
                WorkerObservable workerObservable = new WorkerObservable(workerId, specificationId, context);
                for (WorkerObserver workerObserver : workerObservers) {
                    workerObserver.start(workerObservable);
                }
                startWorkerObserverIsFired.set(true);
            }

            if (LOG.isDebugEnabled()) LOG.debug("[{}] Initialize worker monitor", workerId);
            initializeMonitor();

            // rawdata-client 3.2 (G1): a topic's last message is exact once a producer of it exists, also after a crash,
            // so the producers are opened before the worker or the specification's expressions read what is committed
            openTopics();

            // what an earlier run of the topic committed is never written again (#71)
            if (context.services().contains(CommittedPositions.class)) {
                context.services().get(CommittedPositions.class).read(context.services().get(ContentStore.class),
                        node.configurations().flowContext().topic());
            }

            if (LOG.isDebugEnabled()) LOG.debug("[{}] Before execute worker node", workerId);

            // =============================================================================================
            // Execute start node
            //
            ExecutionContext output = Executor.execute(node, context);
            // the tasks the start node left running, such as parallel entries outside a paginate (#24)
            if (context.services().contains(WorkerTasks.class)) {
                context.services().get(WorkerTasks.class).awaitAll();
            }
            //
            // =============================================================================================

            if (LOG.isDebugEnabled()) LOG.debug("[{}] After execute worker node", workerId);

            if (!keepContentStoreOpenOnWorkerCompletion) {
                if (LOG.isDebugEnabled()) LOG.debug("[{}] Normal close and remove topic", workerId);
                ContentStore contentStore = context.services().get(ContentStore.class);
                contentStore.closeTopic(node.configurations().flowContext().topic());
            }

            if (LOG.isDebugEnabled()) LOG.debug("[{}] Normal shutdown of thread-pool", workerId);
            FixedThreadPool threadPool = context.services().get(FixedThreadPool.class);
            threadPool.shutdownAndAwaitTermination();
            threadPoolIsTerminated.set(true);

            return output;

        } catch (RuntimeException | Error e) {
            if (LOG.isDebugEnabled()) LOG.debug("[{}] Worker RuntimeException or Error occurred!", workerId);
            failure.set(e);
            throw e;
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) LOG.debug("[{}] Worker Exception occurred!", workerId);
            failure.set(e);
            throw new WorkerException(e);
        } finally {
            WorkerStatus workerStatus = null;
            try {
                if (LOG.isDebugEnabled()) LOG.debug("[{}] Finally update WorkerStatus", workerId);
                if (failure.get() == null) {
                    workerStatus = WorkerStatus.COMPLETED;
                } else if (failure.get() instanceof TerminationException || failure.get().getCause() instanceof TerminationException) {
                    workerStatus = WorkerStatus.CANCELED;
                } else {
                    workerStatus = WorkerStatus.FAILED;
                }

                HealthWorkerMonitor monitor = context.services().get(HealthWorkerMonitor.class);
                if (monitor != null) {
                    if (LOG.isDebugEnabled()) LOG.debug("[{}] Finally update worker monitor", workerId);
                    monitor.setStatus(workerStatus);

                    monitor.setEndedTimestamp();

                    if (WorkerStatus.FAILED == workerStatus) {
                        try {
                            HealthThreadsResource healthThreadsResource = new HealthThreadsResource();
                            ObjectNode threadDumpNode = JsonParser.createJsonParser().mapper().convertValue(healthThreadsResource.resource(), ObjectNode.class);
                            monitor.setThreadDumpNode(threadDumpNode);
                            monitor.setFailureCause(CommonUtils.captureStackTrace(failure.get()));
                        } catch (Exception e) {
                            LOG.error("Error occurred during serialization of HealthThreadsResource!\n{}", CommonUtils.captureStackTrace(e));
                            if (failure.get() == null) {
                                failure.set(e);
                            }
                        }
                    }
                }

                if (!threadPoolIsTerminated.get()) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("[{}] Finally terminate thread-pool if worker exception occurred!", workerId);
                    FixedThreadPool threadPool = context.services().get(FixedThreadPool.class);
                    threadPool.shutdownAndAwaitTermination();
                    threadPoolIsTerminated.set(true);
                }

                ContentStore contentStore = context.services().get(ContentStore.class);
                // content this run buffered and did not publish is never published by a later run (#72): after the
                // pool is terminated, so no entry of this run buffers more, and before onFinish, so a run the
                // observers start next is not touched
                String topic = node.configurations().flowContext().topic();
                if (topic != null && contentStore != null && !contentStore.isClosed()) {
                    contentStore.discard(topic);
                }
                if (!keepContentStoreOpenOnWorkerCompletion && !contentStore.isClosed()) {
                    if (LOG.isDebugEnabled()) LOG.debug("[{}] Finally close content store!", workerId);
                    contentStore.closeTopic(node.configurations().flowContext().topic());
                }

                if (startWorkerObserverIsFired.get() && !workerObservers.isEmpty()) {
                    if (LOG.isDebugEnabled()) LOG.debug("[{}] Finally fire observer onFinish", workerId);
                    WorkerObservable workerObservable = new WorkerObservable(workerId, specificationId, context);
                    List<WorkerObserver> workerObserverList = new ArrayList<>(workerObservers);
                    Collections.reverse(workerObserverList);
                    for (WorkerObserver workerObserver : workerObserverList) {
                        workerObserver.finish(workerObservable, workerStatus);
                    }
                    finishWorkerObserverIsFired.set(true);
                }

            } catch (Exception e) {
                if (LOG.isDebugEnabled()) LOG.debug("Finally exception occurred!");
                if (failure.get() != null) {
                    if (LOG.isDebugEnabled()) LOG.debug("[{}] Finally exception occurred addSuppressed", workerId);
                    failure.get().addSuppressed(e);
                } else {
                    if (LOG.isDebugEnabled()) LOG.debug("[{}] Finally setFailure exception occurred", workerId);
                    failure.set(e);
                }

                if (startWorkerObserverIsFired.get() && !finishWorkerObserverIsFired.get() && !workerObservers.isEmpty()) {
                    WorkerObservable workerObservable = new WorkerObservable(workerId, specificationId, context);
                    List<WorkerObserver> workerObserverList = new ArrayList<>(workerObservers);
                    Collections.reverse(workerObserverList);
                    for (WorkerObserver workerObserver : workerObserverList) {
                        workerObserver.finish(workerObservable, workerStatus);
                    }
                    finishWorkerObserverIsFired.set(true);
                }
            }

            Throwable throwable = failure.get();
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else if (throwable instanceof Error) {
                throw (Error) throwable;
            } else if (throwable instanceof Exception) {
                throw new WorkerException(throwable);
            }
        }
    }

    private void initializeMonitor() {
        HealthWorkerMonitor monitor = context.services().get(HealthWorkerMonitor.class);
        if (monitor == null) {
            return;
        }

        monitor.setSpecificationId(specificationId);
        monitor.setName(name);
        Class<?> startFunctionInterface = node.getClass().isInterface() ? node.getClass() : node.getClass().getInterfaces()[0];
        monitor.setStartFunction(startFunctionInterface.getName());
        monitor.setStartFunctionId(((NodeWithId) node).id());

        FixedThreadPool threadPool = context.services().get(FixedThreadPool.class);
        monitor.setThreadPoolInfo(threadPool.asThreadPoolInfo());

        Security security = node.configurations().security();
        if (security != null) {
            monitor.security().setSslBundleName(security.sslBundleName());
        }

        ConfigurationMap configurationMap = context.services().get(ConfigurationMap.class);
        if (configurationMap != null) {
            int httpClientTimeoutSeconds = Integer.parseInt(configurationMap.get(HTTP_CLIENT_TIMEOUT_SECONDS));
            monitor.request().setHttpClientTimeoutSeconds(httpClientTimeoutSeconds);
            int httpRequestTimeoutSeconds = Integer.parseInt(configurationMap.get(HTTP_REQUEST_TIMEOUT_SECONDS));
            monitor.request().setHttpRequestTimeoutSeconds(httpRequestTimeoutSeconds);
        }

        ContentStore contentStore = context.services().get(ContentStore.class);
        if (contentStore != null) {
            String topicName = context.state("global.topic");
            monitor.contentStream().setTopic(topicName);
            monitor.contentStream().setMonitor(contentStore.monitor());
        }

        Headers globalRequestHeaders = context.state(Headers.class);
        if (globalRequestHeaders != null) {
            monitor.request().setHeaders(Masking.of(configurationMap).headers(globalRequestHeaders.asMap()));
        } else {
            Headers globalConfigurationContextRequestHeaders = node.configurations().flowContext().globalContext().state(Headers.class);
            if (globalConfigurationContextRequestHeaders != null) {
                monitor.request().setHeaders(Masking.of(configurationMap).headers(globalConfigurationContextRequestHeaders.asMap()));
            }
        }

        monitor.setStatus(WorkerStatus.RUNNING);
        monitor.setStartedTimestamp();
    }

    /**
     * Runs the worker in the common fork-join pool.
     *
     * @return the future output context of the start node
     */
    public CompletableFuture<ExecutionContext> runAsync() {
        return CompletableFuture
                .supplyAsync(this::run)
                .exceptionally(throwable -> {
                    if (throwable instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    if (throwable instanceof Error error) {
                        throw error;
                    }
                    throw new WorkerException(throwable);
                });
    }

    /**
     * Asks the worker to stop, through its {@code Termination} service.
     */
    public void terminate() {
        Termination termination = context.services().get(Termination.class);
        termination.terminate();
    }

    /**
     * Builds a {@link Worker} from a specification and its configuration, content store, headers, state and
     * certificates.
     */
    public static class WorkerBuilder {

        private static final Logger LOG = LoggerFactory.getLogger(WorkerBuilder.class);
        boolean printExecutionPlan;
        boolean printConfiguration;
        private SpecificationBuilder specificationBuilder;
        private NodeBuilder nodeBuilder;
        private ConfigurationMap configurationMap;
        private final Headers headers = new Headers();
        private final Services services = Services.create();
        // shared by every context of the worker, on every thread of its pool (#46)
        private final Map<Object, Object> globalState = Collections.synchronizedMap(new LinkedHashMap<>());
        private final Map<String, Object> variables = new LinkedHashMap<>();
        private final Map<Object, Object> state = new LinkedHashMap<>();
        private Path sslFactoryScanDirectory;
        private String sslFactoryBundleName;
        private String topicName;
        private final List<WorkerObserver> workerObservers = new ArrayList<>();
        private ContentStore contentStore;
        private boolean keepContentStoreOpenOnWorkerCompletion;
        private Supplier<? extends BusinessSSLResource> businessSSLResourceSupplier;

        /**
         * Creates an empty builder.
         */
        public WorkerBuilder() {
            // the state is in the fields
        }

        /**
         * Sets the specification to run.
         *
         * @param specificationBuilder the specification
         * @return this builder
         */
        public WorkerBuilder specification(SpecificationBuilder specificationBuilder) {
            this.specificationBuilder = specificationBuilder;
            return this;
        }

        /**
         * The specification to run.
         *
         * @return the specification, or {@code null} when none is set
         */
        public SpecificationBuilder getSpecificationBuilder() {
            return specificationBuilder;
        }

        /**
         * Sets a node to run instead of a specification; the node builder's simple class name is used as the specification's
         * id and name.
         *
         * @param nodeBuilder the node
         * @return this builder
         */
        public WorkerBuilder specification(NodeBuilder nodeBuilder) {
            this.nodeBuilder = nodeBuilder;
            return this;
        }

        /**
         * Sets the configuration, copied.
         *
         * @param configurationMap the configuration
         * @return this builder
         */
        public WorkerBuilder configuration(Map<String, String> configurationMap) {
            this.configurationMap = new ConfigurationMap(new LinkedHashMap<>(configurationMap));
            return this;
        }

        /**
         * Adds an observer of the worker's start and finish.
         *
         * @param observer the observer
         * @return this builder
         */
        public WorkerBuilder workerObserver(WorkerObserver observer) {
            this.workerObservers.add(observer);
            return this;
        }

        /**
         * Adds a header to the global request headers.
         *
         * @param name  the header's name
         * @param value the header's value
         * @return this builder
         */
        public WorkerBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        /**
         * Sets a variable of the execution context.
         *
         * @param name  the variable's name
         * @param value the variable's value
         * @return this builder
         */
        public WorkerBuilder variable(String name, Object value) {
            variables.put(name, value);
            return this;
        }

        /**
         * Puts a value into the execution context's global state.
         *
         * @param name  the key
         * @param value the value
         * @return this builder
         */
        public WorkerBuilder globalState(Object name, Object value) {
            globalState.put(name, value);
            return this;
        }

        /**
         * Puts a value into the execution context's state.
         *
         * @param name  the key
         * @param value the value
         * @return this builder
         */
        public WorkerBuilder state(Object name, Object value) {
            state.put(name, value);
            return this;
        }

        /**
         * Makes the worker scan the directory for certificate bundles; the bundle is the one named in the specification's
         * security configuration.
         *
         * @param scanDirectory the directory to scan
         * @return this builder
         */
        public WorkerBuilder buildCertificateFactory(Path scanDirectory) {
            this.sslFactoryScanDirectory = scanDirectory;
            return this;
        }

        /**
         * Makes the worker scan the directory for certificate bundles and use the named one.
         *
         * @param scanDirectory the directory to scan
         * @param bundleName    the bundle's name
         * @return this builder
         */
        public WorkerBuilder buildCertificateFactory(Path scanDirectory, String bundleName) {
            this.sslFactoryScanDirectory = scanDirectory;
            this.sslFactoryBundleName = bundleName;
            return this;
        }

        /**
         * Makes the worker take its certificate bundle from the supplier when no scan directory is set; the bundle is the
         * one named in the specification's security configuration unless a bundle name is set.
         *
         * @param businessSSLResourceSupplier supplies the bundle
         * @return this builder
         */
        public WorkerBuilder useBusinessSSLResourceSupplier(Supplier<? extends BusinessSSLResource> businessSSLResourceSupplier) {
            this.businessSSLResourceSupplier = businessSSLResourceSupplier;
            return this;
        }

        /**
         * Sets the topic to publish to; when none is set, the specification's topic is used.
         *
         * @param topicName the topic
         * @return this builder
         */
        public WorkerBuilder topic(String topicName) {
            this.topicName = topicName;
            return this;
        }

        /**
         * Sets the content store; when none is set, one is made from the configured connector.
         *
         * @param contentStore the content store
         * @return this builder
         */
        public WorkerBuilder contentStore(ContentStore contentStore) {
            this.contentStore = contentStore;
            return this;
        }

        /**
         * Use only when this is intended. If conent store is not closed after each worker is completet, this may lead to memory leaks.
         *
         * @param keepContentStoreOpenOnWorkerCompletion whether the topic is left open when the worker ends
         * @return this builder
         */
        public WorkerBuilder keepContentStoreOpenOnWorkerCompletion(boolean keepContentStoreOpenOnWorkerCompletion) {
            this.keepContentStoreOpenOnWorkerCompletion = keepContentStoreOpenOnWorkerCompletion;
            return this;
        }

        /**
         * Limits the number of iterations of the parallel handler.
         *
         * @param numberOfIterations the maximum number of iterations
         * @return this builder
         */
        public WorkerBuilder stopAtNumberOfIterations(Integer numberOfIterations) {
            state.put(ParallelHandler.MAX_NUMBER_OF_ITERATIONS, numberOfIterations);
            return this;
        }

        /**
         * Makes {@link #build()} log the execution plan.
         *
         * @return this builder
         */
        public WorkerBuilder printExecutionPlan() {
            this.printExecutionPlan = true;
            return this;
        }

        /**
         * Makes {@link #build()} log the specification.
         *
         * @return this builder
         */
        public WorkerBuilder printConfiguration() {
            this.printConfiguration = true;
            return this;
        }


        /**
         * Builds the worker: registers the services (configuration, termination, reordering, thread pool, HTTP client,
         * certificate factory and content store) and creates the execution context. The defaults are an HTTP client
         * timeout of 20 seconds, a request timeout of 15 seconds and the {@code discarding} connector.
         *
         * @return the worker
         * @throws IllegalStateException when no specification or node is set, the start node is missing, a certificate
         *                               source is set but no bundle is named, or no topic is set
         */
        public Worker build() {
            if (configurationMap == null) {
                configurationMap = new ConfigurationMap(new LinkedHashMap<>());
            }
            services.register(ConfigurationMap.class, configurationMap);

            services.register(Termination.class, Termination.create());
            services.register(WorkerTasks.class, new WorkerTasks());
            services.register(CommittedPositions.class, new CommittedPositions());
            services.register(ParallelHandler.Counters.class, new ParallelHandler.Counters());
            services.register(BufferedReordering.class, new BufferedReordering<String>());
            services.register(FixedThreadPool.class, FixedThreadPool.newInstance(
                    configurationMap.contains("data.collector.worker.threads") ? Integer.parseInt(configurationMap.get("data.collector.worker.threads")) : -1)
            );

            String specificationId;
            String name;
            Node targetNode;
            if (specificationBuilder != null) {
                specificationId = specificationBuilder.getId();
                name = specificationBuilder.getName();
                if (printConfiguration && LOG.isInfoEnabled()) {
                    LOG.info("Execute specification:\n{}", specificationBuilder.serializeAsYaml());
                }
                targetNode = specificationBuilder.end().startFunction();
            } else if (nodeBuilder != null) {
                specificationId = nodeBuilder.getClass().getSimpleName();
                name = nodeBuilder.getClass().getSimpleName();
                if (printConfiguration && LOG.isInfoEnabled()) {
                    LOG.info("Execute specification:\n{}", nodeBuilder.serializeAsYaml());
                }
                targetNode = nodeBuilder.build();
            } else {
                throw new IllegalStateException("Flow- or NodeBuilder is undefined!");
            }

            if (targetNode == null) {
                throw new IllegalStateException("Start function is incorrect!");
            }

            if (printExecutionPlan && LOG.isInfoEnabled()) {
                LOG.info("Execution plan:\n{}", targetNode.toPrintableExecutionPlan());
            }

            if ((sslFactoryScanDirectory != null && sslFactoryBundleName == null) || (businessSSLResourceSupplier != null && sslFactoryBundleName == null)) {
                Security nodeSecurityConfig = targetNode.configurations().security();
                if (nodeSecurityConfig == null) {
                    throw new IllegalStateException("Found CertificateFactory, but now bundleName is defined in neither Worker or FlowBuilder");
                }
                sslFactoryBundleName = nodeSecurityConfig.sslBundleName();
            }

            if (!configurationMap.contains(HTTP_CLIENT_TIMEOUT_SECONDS)) {
                configurationMap.put(HTTP_CLIENT_TIMEOUT_SECONDS, "20");
            }

            if (!configurationMap.contains(HTTP_REQUEST_TIMEOUT_SECONDS)) {
                configurationMap.put(HTTP_REQUEST_TIMEOUT_SECONDS, "15");
            }

            configureClient();

            if (topicName == null) {
                FlowContext flowContext = targetNode.configurations().flowContext();
                if (flowContext.topic() == null) {
                    throw new IllegalStateException("Topic name is undefined in neither Worker, Flow- or NodeBuilder!");
                } else {
                    topicName = flowContext.topic();
                }
            }
            globalState.put("global.topic", topicName);

            if (!configurationMap.contains(CONTENT_STREAM_CONNECTOR)) {
                configurationMap.put(CONTENT_STREAM_CONNECTOR, "discarding");
            }

            if (contentStore == null) {
                LOG.warn("ContentStore should be provided to WorkerBuilder! Creating a new instance.");
                contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, configurationMap.get(CONTENT_STREAM_CONNECTOR), configurationMap.asMap());
            }
            services.register(ContentStore.class, ObservedContentStore.of(contentStore));

            if (!headers.asMap().isEmpty()) {
                globalState(Headers.class, headers);
            }

            ExecutionContext executionContext = new ExecutionContext.Builder()
                    .services(services)
                    .variables(variables)
                    .globalState(globalState)
                    .state(state)
                    .build();

            return new Worker(specificationId, name, workerObservers, targetNode, executionContext, keepContentStoreOpenOnWorkerCompletion);
        }

        private void configureClient() {
            Client.Builder clientBuilder = Client.newClientBuilder();
            if (configurationMap.contains("data.collector.http.version")) {
                Client.Version httpVersion = Client.Version.valueOf(configurationMap.get("data.collector.http.version").toUpperCase());
                clientBuilder.version(httpVersion);
            }
            if (configurationMap.contains("data.collector.http.followRedirects")) {
                Client.Redirect redirectPolicy = Client.Redirect.valueOf(configurationMap.get("data.collector.http.followRedirects").toUpperCase());
                clientBuilder.followRedirects(redirectPolicy);
            }

            CertificateFactory sslFactory = createSslFactory();
            if (sslFactory != null) {
                services.register(CertificateFactory.class, sslFactory);
            }
            if (sslFactory != null && sslFactoryBundleName != null) {
                CertificateContext certificateContext = sslFactory.getCertificateContext(sslFactoryBundleName);
                clientBuilder.sslContext(certificateContext.sslContext());
                clientBuilder.x509TrustManager(certificateContext.trustManager());
            }

            clientBuilder.connectTimeout(Duration.ofSeconds(Long.parseLong(configurationMap.get(HTTP_CLIENT_TIMEOUT_SECONDS))));
            Client client = clientBuilder.build();
            LOG.info("Configured HttpClient version: {}", client.version());
            services.register(Client.class, ObservedClient.of(client));
        }

        private CertificateFactory createSslFactory() {
            if (sslFactoryScanDirectory != null) {
                // TODO use businessSSLResourceSupplier and implement load ssl bundle secret.properties
                return CertificateFactory.scanAndCreate(sslFactoryScanDirectory);
            }
            if (businessSSLResourceSupplier == null) {
                return null;
            }
            // the supplier make requests to Google Secret Manager
            return CertificateFactory.create(businessSSLResourceSupplier.get());
        }
    }
}
