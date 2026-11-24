package io.descoped.dc.core.http;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * The interceptors are called directly, as the agent would call them in place of Client.send, Client.sendAsync and
 * GetHandler.execute.
 */
class HttpClientExporterInterceptorTest {

    static final String URL = "http://localhost:9999/api/events";
    static final String LOCATION = new URLInfo(URL).getLocation().orElse("");

    static double sample(String name, String... labels) {
        String[] labelNames = labels.length == 1 ? new String[]{"location"} : new String[]{"location", "statusCode", "emptyResponse"};
        Double value = CollectorRegistry.defaultRegistry.getSampleValue(name, labelNames, labels);
        return value == null ? 0 : value;
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
    void thatSendCountsStartedAndCompletedRequests() throws Exception {
        double started = sample("dc_client_request_started_total", LOCATION);
        double completed = sample("dc_client_request_completed_total", LOCATION, "200", "false");
        Response response = response(200, "body".getBytes());

        assertSame(response, HttpClientExporter.Send.intercept(() -> response, request()));

        assertEquals(started + 1, sample("dc_client_request_started_total", LOCATION));
        assertEquals(completed + 1, sample("dc_client_request_completed_total", LOCATION, "200", "false"));
    }

    @Test
    void thatSendCountsFailedRequests() {
        double failed = sample("dc_client_request_failure_total", LOCATION);
        Request request = request();

        assertThrows(IOException.class, () -> HttpClientExporter.Send.intercept(() -> {
            throw new IOException("refused");
        }, request));

        assertEquals(failed + 1, sample("dc_client_request_failure_total", LOCATION));
    }

    @Test
    void thatSendAsyncCountsCompletedRequests() throws Exception {
        double completed = sample("dc_client_request_completed_total", LOCATION, "204", "true");

        Response response = HttpClientExporter.SendAsync.intercept(() -> CompletableFuture.completedFuture(response(204, new byte[0])), request()).join();

        assertEquals(204, response.statusCode());
        assertEquals(completed + 1, sample("dc_client_request_completed_total", LOCATION, "204", "true"));
    }

    @Test
    void thatSendAsyncCountsFailedRequests() throws Exception {
        double failed = sample("dc_client_request_failure_total", LOCATION);

        CompletableFuture<Response> future = HttpClientExporter.SendAsync.intercept(() -> CompletableFuture.failedFuture(new IllegalStateException("refused")), request());

        CompletionException e = assertThrows(CompletionException.class, future::join);
        assertEquals(IllegalStateException.class, e.getCause().getClass());
        assertEquals(failed + 1, sample("dc_client_request_failure_total", LOCATION));
    }

    @Test
    void thatGetHandlerInterceptorCountsFailures() {
        double failed = sample("dc_client_request_failure_total", LOCATION);
        ExecutionContext context = ExecutionContext.empty().state("PROMETHEUS_METRICS_REQUEST_URL", URL);

        assertThrows(IllegalStateException.class, () -> HttpClientExporter.GetHandlerInterceptor.intercept(() -> {
            throw new IllegalStateException("failed");
        }, context));

        assertEquals(failed + 1, sample("dc_client_request_failure_total", LOCATION));
    }
}
