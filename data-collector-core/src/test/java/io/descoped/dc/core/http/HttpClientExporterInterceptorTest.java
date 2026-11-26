package io.descoped.dc.core.http;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.core.metrics.MetricSamples;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * The advice is called directly, as the code the agent inlines into Client.send, Client.sendAsync and
 * GetHandler.execute calls it.
 */
class HttpClientExporterInterceptorTest {

    static final String URL = "http://localhost:9999/api/events";
    static final String LOCATION = new URLInfo(URL).getLocation().orElse("");

    static double sample(String name, String... labels) {
        String[] labelNames = labels.length == 1 ? new String[]{"location"} : new String[]{"location", "statusCode", "emptyResponse"};
        return MetricSamples.value(name, labelNames, labels);
    }

    static Request request() {
        return Request.newRequestBuilder().url(URL).GET().build();
    }

    static Response response(int statusCode, byte[] body) {
        return new Response() {
            @Override
            public String url() {
                return URL;
            }

            @Override
            public Headers headers() {
                return new Headers();
            }

            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public byte[] body() {
                return body;
            }

            @Override
            public <R> Optional<BodyHandler<R>> bodyHandler() {
                return Optional.empty();
            }

            @Override
            public Optional<Response> previousResponse() {
                return Optional.empty();
            }
        };
    }

    @Test
    void thatSendCountsStartedAndCompletedRequests() {
        double started = sample("dc_client_request_started_total", LOCATION);
        double completed = sample("dc_client_request_completed_total", LOCATION, "200", "false");
        Response response = response(200, "body".getBytes());

        Object observation = HttpClientExporter.Send.enter(request());
        HttpClientExporter.Send.exit(observation, response, null);

        assertEquals(started + 1, sample("dc_client_request_started_total", LOCATION));
        assertEquals(completed + 1, sample("dc_client_request_completed_total", LOCATION, "200", "false"));
    }

    @Test
    void thatSendCountsFailedRequests() {
        double failed = sample("dc_client_request_failure_total", LOCATION);
        Object observation = HttpClientExporter.Send.enter(request());
        HttpClientExporter.Send.exit(observation, null, new IOException("refused"));

        assertEquals(failed + 1, sample("dc_client_request_failure_total", LOCATION));
    }

    @Test
    void thatSendAsyncCountsCompletedRequests() {
        double completed = sample("dc_client_request_completed_total", LOCATION, "204", "true");

        Object observation = HttpClientExporter.SendAsync.enter(request());
        HttpClientExporter.SendAsync.exit(observation, CompletableFuture.completedFuture(response(204, new byte[0])), null);

        assertEquals(completed + 1, sample("dc_client_request_completed_total", LOCATION, "204", "true"));
    }

    @Test
    void thatSendAsyncCountsFailedRequests() {
        double failed = sample("dc_client_request_failure_total", LOCATION);

        CompletableFuture<Response> future = new CompletableFuture<>();
        Object observation = HttpClientExporter.SendAsync.enter(request());
        HttpClientExporter.SendAsync.exit(observation, future, null);
        assertEquals(failed, sample("dc_client_request_failure_total", LOCATION), "counted when the response fails");
        future.completeExceptionally(new IllegalStateException("refused"));

        assertEquals(failed + 1, sample("dc_client_request_failure_total", LOCATION));
    }

    @Test
    void thatGetHandlerInterceptorCountsFailures() {
        double failed = sample("dc_client_request_failure_total", LOCATION);
        ExecutionContext context = ExecutionContext.empty().state("PROMETHEUS_METRICS_REQUEST_URL", URL);

        HttpClientExporter.GetHandlerInterceptor.exit(context, new IllegalStateException("failed"));
        HttpClientExporter.GetHandlerInterceptor.exit(context, null);

        assertEquals(failed + 1, sample("dc_client_request_failure_total", LOCATION));
    }
}
