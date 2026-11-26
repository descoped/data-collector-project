package io.descoped.dc.core.http;

import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.core.metrics.MetricSamples;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * The observed client reports each request to the engine observers, and the Prometheus observer counts it under the
 * metrics of 2.x (issue #25: no dc_client metric was recorded).
 */
class ObservedClientTest {

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

    // a client that answers send with the function, and sendAsync with the future
    static Client client(Function<Request, Response> send, CompletableFuture<Response> sendAsync) {
        return ObservedClient.of(new Client() {
            @Override
            public Version version() {
                return Version.HTTP_1_1;
            }

            @Override
            public Response send(Request request) {
                return send.apply(request);
            }

            @Override
            public <R> Response send(Request request, BodyHandler<R> bodyHandler) {
                return send.apply(request);
            }

            @Override
            public CompletableFuture<Response> sendAsync(Request request) {
                return sendAsync;
            }

            @Override
            public <R> CompletableFuture<Response> sendAsync(Request request, BodyHandler<R> bodyHandler) {
                return sendAsync;
            }

            @Override
            public Object getDelegate() {
                return this;
            }
        });
    }

    @Test
    void thatSendCountsStartedAndCompletedRequests() {
        double started = sample("dc_client_request_started_total", LOCATION);
        double completed = sample("dc_client_request_completed_total", LOCATION, "200", "false");

        client(request -> response(200, "body".getBytes()), null).send(request());

        assertEquals(started + 1, sample("dc_client_request_started_total", LOCATION));
        assertEquals(completed + 1, sample("dc_client_request_completed_total", LOCATION, "200", "false"));
    }

    @Test
    void thatSendCountsFailedRequests() {
        double failed = sample("dc_client_request_failure_total", LOCATION);
        Client client = client(request -> {
            throw new UncheckedIOException(new IOException("refused"));
        }, null);

        assertThrows(UncheckedIOException.class, () -> client.send(request()));

        assertEquals(failed + 1, sample("dc_client_request_failure_total", LOCATION));
    }

    @Test
    void thatSendAsyncCountsCompletedRequests() {
        double completed = sample("dc_client_request_completed_total", LOCATION, "204", "true");

        client(null, CompletableFuture.completedFuture(response(204, new byte[0]))).sendAsync(request());

        assertEquals(completed + 1, sample("dc_client_request_completed_total", LOCATION, "204", "true"));
    }

    @Test
    void thatSendAsyncCountsFailedRequests() {
        double failed = sample("dc_client_request_failure_total", LOCATION);
        CompletableFuture<Response> future = new CompletableFuture<>();

        assertSame(future, client(null, future).sendAsync(request()), "the client's own future");
        assertEquals(failed, sample("dc_client_request_failure_total", LOCATION), "counted when the response fails");
        future.completeExceptionally(new IllegalStateException("refused"));

        assertEquals(failed + 1, sample("dc_client_request_failure_total", LOCATION));
    }

    @Test
    void thatAClientIsObservedOnce() {
        Client client = client(null, null);
        assertSame(client, ObservedClient.of(client));
    }

    static double completed(String location) {
        return sample("dc_client_request_completed_total", location, "200", "false");
    }

    @Test
    void thatTheEnginesClientIsCounted() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            Thread.ofVirtual().start(() -> {
                while (!server.isClosed()) {
                    try (Socket socket = server.accept()) {
                        socket.getInputStream().read(new byte[8192]);
                        socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".getBytes(StandardCharsets.US_ASCII));
                    } catch (IOException _) {
                        // the server is closed
                    }
                }
            });
            String url = "http://127.0.0.1:" + server.getLocalPort() + "/metrics-test";
            String location = new URLInfo(url).getLocation().orElse("");
            double before = completed(location);

            Client client = ObservedClient.of(Client.newClient());
            assertNotNull(client.send(Request.newRequestBuilder().GET().url(url).build()));
            assertNotNull(client.sendAsync(Request.newRequestBuilder().GET().url(url).build()).get());

            // the asynchronous response is counted when its future completes, which may be just after get() returns
            for (int i = 0; i < 50 && completed(location) < before + 2; i++) {
                Thread.sleep(20);
            }
            assertEquals(before + 2, completed(location));
        }
    }
}
