package io.descoped.dc.application.controller;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.application.server.UndertowApplication;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import static io.descoped.dc.api.util.CommonUtils.captureStackTrace;
import static io.descoped.dc.application.controller.CORSHandlerTest.findFree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsControllerTest.class);

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String TOKEN = "metrics-test-token-0123456789abcdef0123";

    private static UndertowApplication server;

    @BeforeAll
    static void beforeAll() {
        DynamicConfiguration configuration = DynamicConfiguration.builder()
                .value("prometheus.defaultExports.disabled", "true")
                .value("http.authentication.bearer.token", TOKEN)
                .build();
        server = UndertowApplication.initializeUndertowApplication(configuration, findFree(), null);
        server.start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    public static HttpResponse GET(String uri, String... headers) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(
                    URI.create("http://" + server.getHost() + ":" + server.getPort() + uri))
                    .header("Authorization", "Bearer " + TOKEN)
                    .GET();
            if (headers != null && headers.length > 1) {
                requestBuilder.headers(headers);
            }
            java.net.http.HttpResponse<String> response = client.send(requestBuilder.build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            return new HttpResponse(response.statusCode(), response.body());
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    @Test
    void thatMetricsAreHealth() {
        HttpResponse response = GET("/metrics/-/healthy");
        assertEquals(200, response.statusCode);
        assertEquals("Exporter is Healthy.", response.body);
    }

    @Test
    void thatMetricsAreReported() {
        HttpResponse response = GET("/metrics");
        assertEquals(200, response.statusCode);
        LOG.trace("body:\n{}", response.body);
    }

    // name[] selects samples by the names /metrics shows: a counter with _total
    @Test
    void thatNameParametersSelectTheMetrics() {
        Counter requests = Counter.builder().name("ns_foo_requests").help("Requests").register();
        Gauge time = Gauge.builder().name("ns_foo_time").help("Time").register();
        requests.inc();
        time.set(1);

        HttpResponse response = GET("/metrics?name%5B%5D=ns_foo_requests_total");
        assertEquals(200, response.statusCode);
        assertTrue(response.body.contains("ns_foo_requests_total 1.0"), response.body);
        assertFalse(response.body.contains("ns_foo_time"), response.body);
    }

    static class HttpResponse {
        public final int statusCode;
        public final String body;

        public HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
