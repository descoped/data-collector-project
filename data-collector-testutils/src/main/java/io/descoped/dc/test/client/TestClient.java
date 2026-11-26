package io.descoped.dc.test.client;

import io.descoped.dc.test.server.TestUriResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * An HTTP client for the test server: each method sends one request and fails with {@link TestClientException}.
 */
public final class TestClient {

    private static final Logger LOG = LoggerFactory.getLogger(TestClient.class);

    private final TestUriResolver server;
    private final HttpClient client;

    private TestClient(TestUriResolver server) {
        this.server = server;
        this.client = HttpClient.newBuilder().build();
    }

    /**
     * Creates a client.
     *
     * @param server the server whose URLs the client resolves
     * @return the client
     */
    public static TestClient create(TestUriResolver server) {
        return new TestClient(server);
    }

    /**
     * The resolver of the test server's URLs.
     *
     * @return the resolver
     */
    public TestUriResolver getTestUriResolver() {
        return server;
    }

    static String captureStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Sends an OPTIONS request.
     *
     * @param uri               the path on the test server
     * @param headersKeyAndValue the headers, as alternating names and values
     * @return the response, its body as a string
     */
    public ResponseHelper<String> options(String uri, String... headersKeyAndValue) {
        return options(uri, HttpResponse.BodyHandlers.ofString(), headersKeyAndValue);
    }

    /**
     * Sends an OPTIONS request.
     *
     * @param uri               the path on the test server
     * @param bodyHandler       the handler of the response body
     * @param headersKeyAndValue the headers, as alternating names and values
     * @param <R>               the body's type
     * @return the response
     */
    public <R> ResponseHelper<R> options(String uri, HttpResponse.BodyHandler<R> bodyHandler, String... headersKeyAndValue) {
        return send(() -> HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                .headers(headersKeyAndValue)
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build(), bodyHandler);
    }

    /**
     * Sends a HEAD request.
     *
     * @param uri     the path on the test server
     * @return the response, its body as a string
     */
    public ResponseHelper<String> head(String uri) {
        return head(uri, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a HEAD request.
     *
     * @param uri         the path on the test server
     * @param bodyHandler the handler of the response body
     * @param <R>         the body's type
     * @return the response
     */
    public <R> ResponseHelper<R> head(String uri, HttpResponse.BodyHandler<R> bodyHandler) {
        return send(() -> HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build(), bodyHandler);
    }

    /**
     * Sends a PUT request without a body.
     *
     * @param uri     the path on the test server
     * @return the response, its body as a string
     */
    public ResponseHelper<String> put(String uri) {
        return put(uri, HttpRequest.BodyPublishers.noBody(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a PUT request with a body.
     *
     * @param uri     the path on the test server
     * @param body    the body
     * @return the response, its body as a string
     */
    public ResponseHelper<String> put(String uri, String body) {
        return put(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a PUT request.
     *
     * @param uri           the path on the test server
     * @param bodyPublisher the publisher of the request body
     * @param bodyHandler   the handler of the response body
     * @param <R>           the body's type
     * @return the response
     */
    public <R> ResponseHelper<R> put(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        return send(() -> HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                .PUT(bodyPublisher)
                .build(), bodyHandler);
    }

    /**
     * Sends a POST request without a body.
     *
     * @param uri     the path on the test server
     * @return the response, its body as a string
     */
    public ResponseHelper<String> post(String uri) {
        return post(uri, HttpRequest.BodyPublishers.noBody(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a POST request with a body.
     *
     * @param uri     the path on the test server
     * @param body    the body
     * @return the response, its body as a string
     */
    public ResponseHelper<String> post(String uri, String body) {
        return post(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a POST request.
     *
     * @param uri           the path on the test server
     * @param bodyPublisher the publisher of the request body
     * @param bodyHandler   the handler of the response body
     * @param <R>           the body's type
     * @return the response
     */
    public <R> ResponseHelper<R> post(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        return send(() -> HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                .POST(bodyPublisher)
                .build(), bodyHandler);
    }

    /**
     * Sends a POST request with a JSON body.
     *
     * @param uri           the path on the test server
     * @param bodyPublisher the publisher of the request body
     * @param bodyHandler   the handler of the response body
     * @param <R>           the body's type
     * @return the response
     */
    public <R> ResponseHelper<R> postJson(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        return send(() -> HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                .POST(bodyPublisher)
                .header("Content-Type", "application/json")
                .build(), bodyHandler);
    }

    /**
     * Sends a POST request with a URL-encoded form.
     *
     * @param uri           the path on the test server
     * @param bodyPublisher the publisher of the request body
     * @param bodyHandler   the handler of the response body
     * @param <R>           the body's type
     * @return the response
     */
    public <R> ResponseHelper<R> postForm(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        return send(() -> HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                .POST(bodyPublisher)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build(), bodyHandler);
    }

    /**
     * Sends a GET request.
     *
     * @param uri     the path on the test server
     * @param headers the headers, as alternating names and values
     * @return the response, its body as a string
     */
    public ResponseHelper<String> get(String uri, String... headers) {
        return get(uri, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8), headers);
    }

    /**
     * Sends a GET request.
     *
     * @param uri         the path on the test server
     * @param bodyHandler the handler of the response body
     * @param headers     the headers, as alternating names and values
     * @param <R>         the body's type
     * @return the response
     */
    public <R> ResponseHelper<R> get(String uri, HttpResponse.BodyHandler<R> bodyHandler, String... headers) {
        return send(() -> {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                    .GET();
            if (headers.length > 0) {
                requestBuilder.headers(headers);
            }
            return requestBuilder.build();
        }, bodyHandler);
    }

    /**
     * Sends a DELETE request.
     *
     * @param uri     the path on the test server
     * @return the response, its body as a string
     */
    public ResponseHelper<String> delete(String uri) {
        return delete(uri, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a DELETE request.
     *
     * @param uri         the path on the test server
     * @param bodyHandler the handler of the response body
     * @param <R>         the body's type
     * @return the response
     */
    public <R> ResponseHelper<R> delete(String uri, HttpResponse.BodyHandler<R> bodyHandler) {
        return send(() -> HttpRequest.newBuilder(URI.create(server.testURL(uri)))
                .DELETE()
                .build(), bodyHandler);
    }

    // the request is built and sent in one place; an interrupted send keeps the thread's interrupt flag
    private <R> ResponseHelper<R> send(Supplier<HttpRequest> request, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            return new ResponseHelper<>(client.send(request.get(), bodyHandler));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Error: {}", captureStackTrace(e));
            throw new TestClientException(e);
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new TestClientException(e);
        }
    }
}
