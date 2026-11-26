package io.descoped.dc.core.http;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import net.bytebuddy.asm.Advice;

import java.util.concurrent.CompletableFuture;

/**
 * The HTTP client's metrics, recorded by advice that {@link HttpClientAgent} inlines into the client and the
 * {@code GetHandler}:
 * <p>
 * request_started
 * request_completed
 * request_failure
 * request_duration
 * <p>
 * The advice is inlined, so it calls only public methods here; it needs no helper classes in the target's package,
 * which a named module on JDK 27 cannot be given.
 */
public final class HttpClientExporter {

    private static final String SUBSYSTEM = "client";
    private static final String LOCATION = "location";

    private HttpClientExporter() {
    }

    static final Counter requestStartedCount = Counter.build("request_started", "Started requests")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(LOCATION)
            .register();

    static final Counter requestCompletedCount = Counter.build("request_completed", "Completed requests")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(LOCATION, "statusCode", "emptyResponse")
            .register();

    static final Counter requestFailureCount = Counter.build("request_failure", "Failed requests")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(LOCATION)
            .register();

    static final Histogram requestDurationHistogram = Histogram.build("request_duration", "Request duration")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(LOCATION)
            .register();

    // a request being measured: its location label and its timer
    private record Observation(String location, Histogram.Timer timer) {
    }

    /**
     * Starts measuring a request.
     *
     * @param request the request
     * @return the observation, passed to {@link #completed} or {@link #observe}
     */
    public static Object started(Request request) {
        // prometheus: default to empty location so it doesn't throw NPE
        String location = new URLInfo(request == null ? null : request.url()).getLocation().orElse("");
        Histogram.Timer timer = requestDurationHistogram.labels(location).startTimer();
        requestStartedCount.labels(location).inc();
        return new Observation(location, timer);
    }

    /**
     * Ends measuring a request that was sent and answered, or that failed.
     *
     * @param observation the observation from {@link #started}
     * @param response    the response, or {@code null}
     * @param thrown      what the request threw, or {@code null}
     */
    public static void completed(Object observation, Response response, Throwable thrown) {
        if (!(observation instanceof Observation started)) {
            return;
        }
        try {
            if (thrown != null) {
                requestFailureCount.labels(started.location()).inc();
            } else {
                requestCompletedCount.labels(started.location(), statusCode(response), emptyResponse(response)).inc();
            }
        } finally {
            started.timer().observeDuration();
        }
    }

    /**
     * Ends measuring an asynchronous request when its response arrives.
     *
     * @param observation the observation from {@link #started}
     * @param future      the response to come, or {@code null}
     * @param thrown      what sending the request threw, or {@code null}
     */
    public static void observe(Object observation, CompletableFuture<Response> future, Throwable thrown) {
        if (thrown != null || future == null) {
            completed(observation, null, thrown != null ? thrown : new IllegalStateException("No response"));
            return;
        }
        future.whenComplete((response, failure) -> completed(observation, response, failure));
    }

    /**
     * Counts a failure of a {@code GetHandler}.
     *
     * @param context the handler's input
     * @param thrown  what it threw, or {@code null}
     */
    public static void handlerCompleted(ExecutionContext context, Throwable thrown) {
        if (thrown == null) {
            return;
        }
        String url = context != null ? context.state("PROMETHEUS_METRICS_REQUEST_URL") : null;
        // prometheus: default to empty location so it doesn't throw NPE
        requestFailureCount.labels(new URLInfo(url).getLocation().orElse("")).inc();
    }

    private static String statusCode(Response response) {
        return response == null ? "-1" : String.valueOf(response.statusCode());
    }

    private static String emptyResponse(Response response) {
        return response == null || !(response.body() != null && response.body().length > 0) ? "true" : "false";
    }

    /**
     * Advice for {@code Client.send}.
     */
    public static final class Send {

        private Send() {
        }

        /**
         * Starts measuring.
         *
         * @param request the request
         * @return the observation
         */
        @Advice.OnMethodEnter
        public static Object enter(@Advice.Argument(0) Request request) {
            return started(request);
        }

        /**
         * Ends measuring.
         *
         * @param observation the observation
         * @param response    the response
         * @param thrown      what was thrown
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Enter Object observation, @Advice.Return Response response, @Advice.Thrown Throwable thrown) {
            completed(observation, response, thrown);
        }
    }

    /**
     * Advice for {@code Client.sendAsync}.
     */
    public static final class SendAsync {

        private SendAsync() {
        }

        /**
         * Starts measuring.
         *
         * @param request the request
         * @return the observation
         */
        @Advice.OnMethodEnter
        public static Object enter(@Advice.Argument(0) Request request) {
            return started(request);
        }

        /**
         * Ends measuring when the response arrives.
         *
         * @param observation the observation
         * @param future      the response to come
         * @param thrown      what was thrown
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Enter Object observation, @Advice.Return CompletableFuture<Response> future, @Advice.Thrown Throwable thrown) {
            observe(observation, future, thrown);
        }
    }

    /**
     * Advice for {@code GetHandler.execute}.
     */
    public static final class GetHandlerInterceptor {

        private GetHandlerInterceptor() {
        }

        /**
         * Counts a failure.
         *
         * @param context the handler's input
         * @param thrown  what was thrown
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Argument(0) ExecutionContext context, @Advice.Thrown Throwable thrown) {
            handlerCompleted(context, thrown);
        }
    }
}
