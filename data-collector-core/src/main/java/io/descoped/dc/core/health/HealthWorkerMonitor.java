package io.descoped.dc.core.health;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.content.HealthContentStreamMonitor;
import io.descoped.dc.api.health.HealthResourceUtils;
import io.descoped.dc.core.executor.WorkerStatus;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The health state of one worker, updated while it runs. {@link #build()} takes a snapshot of it as a
 * {@link WorkerInfo}.
 */
public class HealthWorkerMonitor {

    final AtomicReference<WorkerStatus> statusRef = new AtomicReference<>();
    final AtomicReference<String> specificationIdRef = new AtomicReference<>();
    final AtomicReference<String> nameRef = new AtomicReference<>();
    final AtomicReference<String> startFunctionRef = new AtomicReference<>();
    final AtomicReference<String> startFunctionIdRef = new AtomicReference<>();
    final AtomicLong startedRef = new AtomicLong(0);
    final AtomicLong endedRef = new AtomicLong(0);
    final AtomicReference<String> failureCauseRef = new AtomicReference<>();
    final Security security = new Security();
    final Request request = new Request(startedRef::get);
    final ContentStore contentStore = new ContentStore();
    final Map<String, Object> threadPoolInfo = new LinkedHashMap<>();
    final AtomicReference<ObjectNode> threadDumpNodeRef = new AtomicReference<>();

    /**
     * Creates the monitor.
     */
    public HealthWorkerMonitor() {
        // the state is in the fields
    }

    /**
     * The worker's status.
     *
     * @return the status, or {@code null} when none is set
     */
    public WorkerStatus status() {
        return statusRef.get();
    }

    /**
     * Sets the worker's status.
     *
     * @param status the status
     */
    public void setStatus(WorkerStatus status) {
        statusRef.set(status);
    }

    /**
     * Sets the id of the specification the worker runs.
     *
     * @param specificationId the specification's id
     */
    public void setSpecificationId(String specificationId) {
        specificationIdRef.set(specificationId);
    }

    /**
     * Sets the name of the specification the worker runs.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.nameRef.set(name);
    }

    /**
     * Sets the type of the specification's start node.
     *
     * @param function the start node's type
     */
    public void setStartFunction(String function) {
        this.startFunctionRef.set(function);
    }

    /**
     * Sets the id of the specification's start node.
     *
     * @param functionId the start node's id
     */
    public void setStartFunctionId(String functionId) {
        this.startFunctionIdRef.set(functionId);
    }

    /**
     * Records the current time as the time the worker started.
     */
    public void setStartedTimestamp() {
        startedRef.set(Instant.now().toEpochMilli());
    }

    /**
     * Records the current time as the time the worker ended.
     */
    public void setEndedTimestamp() {
        endedRef.set(Instant.now().toEpochMilli());
    }

    /**
     * Sets the cause of the worker's failure.
     *
     * @param failureCause the cause
     */
    public void setFailureCause(String failureCause) {
        failureCauseRef.set(failureCause);
    }

    /**
     * Sets the thread dump taken for the worker.
     *
     * @param threadDumpNode the thread dump
     */
    public void setThreadDumpNode(ObjectNode threadDumpNode) {
        threadDumpNodeRef.set(threadDumpNode);
    }

    /**
     * The security section.
     *
     * @return the security section
     */
    public Security security() {
        return security;
    }

    /**
     * The request section.
     *
     * @return the request section
     */
    public Request request() {
        return request;
    }

    /**
     * The content stream section.
     *
     * @return the content stream section
     */
    public ContentStore contentStream() {
        return contentStore;
    }

    /**
     * Adds the entries to the thread pool information.
     *
     * @param threadPoolInfo the thread pool information
     */
    public void setThreadPoolInfo(Map<String, Object> threadPoolInfo) {
        this.threadPoolInfo.putAll(threadPoolInfo);
    }

    /**
     * Takes a snapshot of the worker's health. The content stream section needs a monitor set with
     * {@link ContentStore#setMonitor}.
     *
     * @return the snapshot
     */
    public WorkerInfo build() {
        return new WorkerInfo(
                statusRef.get(),
                specificationIdRef.get(),
                nameRef.get(),
                startedRef.get() == 0L ? null : Instant.ofEpochMilli(startedRef.get()).toString(),
                endedRef.get() == 0L ? null : Instant.ofEpochMilli(endedRef.get()).toString(),
                startedRef.get() == 0L ? null : HealthResourceUtils.durationAsString(startedRef.get()),
                startFunctionRef.get(),
                startFunctionIdRef.get(),
                failureCauseRef.get(),
                security.build(),
                request.build(),
                contentStore.build(),
                threadPoolInfo,
                threadDumpNodeRef.get());
    }

    /**
     * The worker's security section.
     */
    public static class Security {
        String sslBundleName;

        /**
         * Creates the section.
         */
        public Security() {
            // the state is in the fields
        }

        /**
         * Sets the name of the SSL bundle the worker's client uses.
         *
         * @param sslBundleName the bundle's name
         */
        public void setSslBundleName(String sslBundleName) {
            this.sslBundleName = sslBundleName;
        }

        SecurityInfo build() {
            return new SecurityInfo(sslBundleName);
        }
    }

    /**
     * The worker's request section: timeouts, headers and request counters.
     */
    public static class Request {
        final AtomicInteger httpClientTimeoutSecondsRef = new AtomicInteger();
        final AtomicInteger httpRequestTimeoutSecondsRef = new AtomicInteger();

        final Map<String, List<String>> requestHeaders = new LinkedHashMap<>();

        final AtomicInteger prefetchThresholdRef = new AtomicInteger(0);
        final AtomicLong prefetchCountRef = new AtomicLong(0);
        final AtomicLong totalExpectedCountRef = new AtomicLong(0);
        final AtomicLong totalCompletedCountRef = new AtomicLong(0);

        final AtomicLong completedRequestCountRef = new AtomicLong(0);
        final AtomicLong lastRequestDurationMilliSecondsRef = new AtomicLong(0);
        final AtomicLong requestDurationMilliSecondsRef = new AtomicLong(0);
        final AtomicLong requestRetryOnFailureCountRef = new AtomicLong(0);

        final Supplier<Long> startedInMillisSupplier;

        /**
         * Creates the section.
         *
         * @param startedInMillisSupplier gives the time the worker started, in epoch milliseconds, for the request rate
         */
        public Request(Supplier<Long> startedInMillisSupplier) {
            this.startedInMillisSupplier = startedInMillisSupplier;
        }

        /**
         * Sets the HTTP client's timeout.
         *
         * @param timeout the timeout in seconds
         */
        public void setHttpClientTimeoutSeconds(int timeout) {
            httpClientTimeoutSecondsRef.set(timeout);
        }

        /**
         * Sets the HTTP request's timeout.
         *
         * @param timeout the timeout in seconds
         */
        public void setHttpRequestTimeoutSeconds(int timeout) {
            httpRequestTimeoutSecondsRef.set(timeout);
        }

        /**
         * Adds the request headers.
         *
         * @param requestHeaders the headers, values by name
         */
        public void setHeaders(Map<String, List<String>> requestHeaders) {
            this.requestHeaders.putAll(requestHeaders);
        }

        /**
         * Counts a completed request.
         */
        public void incrementCompletedRequestCount() {
            completedRequestCountRef.incrementAndGet();
        }

        /**
         * Sets the duration of the last request.
         *
         * @param durationMilliSeconds the duration in milliseconds
         */
        public void updateLastRequestDurationMillisSeconds(long durationMilliSeconds) {
            lastRequestDurationMilliSecondsRef.set(durationMilliSeconds);
        }

        /**
         * Adds the duration of a request to the total request duration.
         *
         * @param durationMilliSeconds the duration in milliseconds
         */
        public void addRequestDurationMillisSeconds(long durationMilliSeconds) {
            requestDurationMilliSecondsRef.addAndGet(durationMilliSeconds);
        }

        /**
         * Counts a request retried after a failure.
         */
        public void incrementRequestRetryOnFailureCount() {
            requestRetryOnFailureCountRef.incrementAndGet();
        }

        /**
         * Sets the prefetch threshold.
         *
         * @param threshold the threshold
         */
        public void setPrefetchThreshold(int threshold) {
            prefetchThresholdRef.set(threshold);
        }

        /**
         * Counts a prefetch.
         */
        public void incrementPrefetchCount() {
            prefetchCountRef.incrementAndGet();
        }

        /**
         * Decrements the prefetch count.
         */
        public void decrementPrefetchCount() {
            prefetchCountRef.decrementAndGet();
        }

        /**
         * Adds to the total expected count.
         *
         * @param expectedCount the count to add
         */
        public void updateTotalExpectedCount(Integer expectedCount) {
            totalExpectedCountRef.addAndGet(expectedCount);
        }

        /**
         * Adds to the total completed count.
         *
         * @param completedCount the count to add
         */
        public void updateTotalCompletedCount(Integer completedCount) {
            totalCompletedCountRef.addAndGet(completedCount);
        }

        RequestInfo build() {
            long now = System.currentTimeMillis();
            Float averageRequestPerSecond = HealthResourceUtils.divide(completedRequestCountRef.get(), (now - startedInMillisSupplier.get()) / 1000);
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.UP);
            averageRequestPerSecond = Float.parseFloat(df.format(averageRequestPerSecond));

            float avgRequestDurationMillis = HealthResourceUtils.divide(requestDurationMilliSecondsRef.get(), completedRequestCountRef.get());
            float averageRequestDurationMillis = (avgRequestDurationMillis);

            return new RequestInfo(
                    httpClientTimeoutSecondsRef.get(),
                    httpRequestTimeoutSecondsRef.get(),
                    requestHeaders,
                    completedRequestCountRef.get(),
                    averageRequestPerSecond,
                    lastRequestDurationMilliSecondsRef.get(),
                    Math.round(averageRequestDurationMillis),
                    requestRetryOnFailureCountRef.get(),
                    prefetchThresholdRef.get(),
                    prefetchCountRef.get(),
                    totalExpectedCountRef.get(),
                    totalCompletedCountRef.get()
            );
        }
    }

    /**
     * The worker's content stream section.
     */
    public static class ContentStore {
        final AtomicBoolean hasNotSetStartPositionRef = new AtomicBoolean(true);
        final AtomicReference<String> startPositionRef = new AtomicReference<>();
        final AtomicReference<String> lastPositionRef = new AtomicReference<>();
        final AtomicReference<HealthContentStreamMonitor> contentStreamMonitorRef = new AtomicReference<>();
        String topic;

        /**
         * Creates the section.
         */
        public ContentStore() {
            // the state is in the fields
        }

        /**
         * Sets the topic the worker publishes to.
         *
         * @param topic the topic
         */
        public void setTopic(String topic) {
            this.topic = topic;
        }

        /**
         * Whether no start position is set yet.
         *
         * @return {@code true} until a start position is set
         */
        public boolean hasNotSetStartPosition() {
            return hasNotSetStartPositionRef.get();
        }

        /**
         * Sets the start position, unless one is already set.
         *
         * @param startPosition the start position
         */
        public void setStartPosition(String startPosition) {
            if (startPositionRef.compareAndSet(null, startPositionRef.get())) {
                hasNotSetStartPositionRef.set(false);
                startPositionRef.set(startPosition);
            }
        }

        /**
         * Sets the last position.
         *
         * @param lastPosition the last position
         */
        public void setLastPosition(String lastPosition) {
            lastPositionRef.set(lastPosition);
        }

        HealthContentStreamMonitor monitor() {
            return contentStreamMonitorRef.get();
        }

        /**
         * Sets the monitor of the content stream.
         *
         * @param monitor the monitor
         */
        public void setMonitor(HealthContentStreamMonitor monitor) {
            contentStreamMonitorRef.set(monitor);
        }

        ContentStoreInfo build() {
            return new ContentStoreInfo(
                    topic,
                    startPositionRef.get(),
                    lastPositionRef.get(),
                    contentStreamMonitorRef.get().build()
            );
        }
    }

    /**
     * A snapshot of a worker's health.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings("WeakerAccess")
    public static class WorkerInfo {
        /**
         * The worker's status; not rendered.
         */
        @JsonIgnore
        public final WorkerStatus status;
        /**
         * The specification's id.
         */
        @JsonProperty("specificationId")
        public final String specificationId;
        /**
         * The specification's name.
         */
        @JsonProperty("name")
        public final String name;
        /**
         * The time the worker started, or {@code null}.
         */
        @JsonProperty("started")
        public final String started;
        /**
         * The time the worker ended, or {@code null}.
         */
        @JsonProperty("ended")
        public final String ended;
        /**
         * The time since the worker started, or {@code null} when it has not started.
         */
        @JsonProperty("duration")
        public final String duration;
        /**
         * The type of the start node.
         */
        @JsonProperty("start-function")
        public final String startFunction;
        /**
         * The id of the start node.
         */
        @JsonProperty("start-function-id")
        public final String startFunctionId;
        /**
         * The cause of the worker's failure, or {@code null}.
         */
        @JsonProperty("failure-cause")
        public final String failureCause;
        /**
         * The security section.
         */
        @JsonProperty("security")
        public final SecurityInfo securityInfo;
        /**
         * The request section.
         */
        @JsonProperty("request-info")
        public final RequestInfo requestInfo;
        /**
         * The content stream section.
         */
        @JsonProperty("content-stream")
        public final ContentStoreInfo contentStoreInfo;
        /**
         * The thread pool information.
         */
        @JsonProperty("thread-pool")
        public final Map<String, Object> threadPoolInfo;
        /**
         * The thread dump, or {@code null}.
         */
        @JsonProperty("thread-dump")
        public final ObjectNode threadDumpNode;

        WorkerInfo(WorkerStatus status,
                   String specificationId,
                   String name,
                   String started,
                   String ended,
                   String duration,
                   String startFunction,
                   String startFunctionId,
                   String failureCause,
                   SecurityInfo securityInfo,
                   RequestInfo requestInfo,
                   ContentStoreInfo contentStoreInfo,
                   Map<String, Object> threadPoolInfo,
                   ObjectNode threadDumpNode) {
            this.status = status;
            this.specificationId = specificationId;
            this.name = name;
            this.started = started;
            this.ended = ended;
            this.duration = duration;
            this.startFunction = startFunction;
            this.startFunctionId = startFunctionId;
            this.failureCause = failureCause;
            this.securityInfo = securityInfo;
            this.requestInfo = requestInfo;
            this.contentStoreInfo = contentStoreInfo;
            this.threadPoolInfo = threadPoolInfo;
            this.threadDumpNode = threadDumpNode;
        }

        /**
         * The specification's name.
         *
         * @return the name
         */
        @JsonIgnore
        public String getName() {
            return name;
        }
    }

    /**
     * A snapshot of a worker's security section.
     */
    @SuppressWarnings("WeakerAccess")
    public static class SecurityInfo {
        /**
         * The name of the SSL bundle, or {@code null}.
         */
        @JsonProperty("ssl-bundle-name")
        public final String sslBundleName;

        SecurityInfo(String sslBundleName) {
            this.sslBundleName = sslBundleName;
        }
    }

    /**
     * A snapshot of a worker's request section.
     */
    @SuppressWarnings("WeakerAccess")
    public static class RequestInfo {
        /**
         * The HTTP client's timeout in seconds.
         */
        @JsonProperty("http-client-timeout-seconds")
        public final Integer httpClientTimeoutSeconds;
        /**
         * The HTTP request's timeout in seconds.
         */
        @JsonProperty("http-request-timeout-seconds")
        public final Integer httpRequestTimeoutSeconds;
        /**
         * The request headers, values by name.
         */
        @JsonProperty("request-headers")
        public final Map<String, List<String>> requestHeaders;
        /**
         * The number of completed requests.
         */
        @JsonProperty("request-count")
        public final Long requestCount;
        /**
         * The average number of completed requests per second since the worker started.
         */
        @JsonProperty("avg-requests-per-second")
        public final Float averageRequestPerSecond;
        /**
         * The duration of the last request in milliseconds.
         */
        @JsonProperty("last-request-duration-millis")
        public final Long lastRequestDurationMillis;
        /**
         * The average request duration in milliseconds.
         */
        @JsonProperty("avg-request-duration-millis")
        public final Integer averageRequestDurationMillis;
        /**
         * The number of requests retried after a failure.
         */
        @JsonProperty("retry-on-failure-count")
        public final Long retryOnFailureCount;
        /**
         * The prefetch threshold.
         */
        @JsonProperty("prefetch-threshold")
        public final int prefetchThreshold;
        /**
         * The prefetch count.
         */
        @JsonProperty("prefetch-count")
        public final Long prefetchCount;
        /**
         * The total expected count.
         */
        @JsonProperty("expected-count")
        public final Long expectedCount;
        /**
         * The total completed count.
         */
        @JsonProperty("completed-count")
        public final Long completedCount;

        RequestInfo(Integer httpClientTimeoutSeconds, Integer httpRequestTimeoutSeconds, Map<String, List<String>> requestHeaders, Long requestCount, Float averageRequestPerSecond, Long lastRequestDurationMillis, Integer averageRequestDurationMillis, Long retryOnFailureCount, int prefetchThreshold
                , Long prefetchCount, Long expectedCount, Long completedCount) {
            this.httpClientTimeoutSeconds = httpClientTimeoutSeconds;
            this.httpRequestTimeoutSeconds = httpRequestTimeoutSeconds;
            this.requestHeaders = requestHeaders;
            this.requestCount = requestCount;
            this.averageRequestPerSecond = averageRequestPerSecond;
            this.lastRequestDurationMillis = lastRequestDurationMillis;
            this.averageRequestDurationMillis = averageRequestDurationMillis;
            this.retryOnFailureCount = retryOnFailureCount;
            this.prefetchThreshold = prefetchThreshold;
            this.prefetchCount = prefetchCount;
            this.expectedCount = expectedCount;
            this.completedCount = completedCount;
        }
    }

    /**
     * A snapshot of a worker's content stream section.
     */
    @JsonPropertyOrder({"topic", "start-position", "last-position"})
    @SuppressWarnings("WeakerAccess")
    public static class ContentStoreInfo {
        /**
         * The topic.
         */
        @JsonProperty("topic")
        public final String topic;
        /**
         * The start position.
         */
        @JsonProperty("start-position")
        public final String startPosition;
        /**
         * The last position.
         */
        @JsonProperty("last-position")
        public final String lastPosition;
        /**
         * The content stream's own information, rendered inline.
         */
        @JsonUnwrapped
        public final HealthContentStreamMonitor.ContentStreamInfo contentStream;

        ContentStoreInfo(String topic, String startPosition, String lastPosition, HealthContentStreamMonitor.ContentStreamInfo contentStream) {
            this.topic = topic;
            this.startPosition = startPosition;
            this.lastPosition = lastPosition;
            this.contentStream = contentStream;
        }

        /**
         * The topic.
         *
         * @return the topic
         */
        @JsonIgnore
        public String getTopic() {
            return topic;
        }
    }

}
