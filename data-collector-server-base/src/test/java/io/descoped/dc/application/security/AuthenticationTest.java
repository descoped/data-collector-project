package io.descoped.dc.application.security;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.application.server.UndertowApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationTest {

    static final String TOKEN = "authentication-test-token";

    static final HttpClient client = HttpClient.newHttpClient();
    static UndertowApplication server;

    static int findFree() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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

    static HttpResponse<String> send(String method, String path, String... headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://" + server.getHost() + ":" + server.getPort() + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (headers.length > 0) {
            builder.headers(headers);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void thatEveryEndpointRequiresTheToken() throws Exception {
        for (String path : new String[]{"/ping", "/health", "/health/alive", "/health/ready", "/metrics"}) {
            HttpResponse<String> response = send("GET", path);
            assertEquals(401, response.statusCode(), path);
            assertEquals("Bearer realm=\"data-collector\"", response.headers().firstValue("WWW-Authenticate").orElse(null), path);
            assertEquals(401, send("GET", path, "Authorization", "Bearer wrong-token").statusCode(), path);
            assertEquals(401, send("GET", path, "Authorization", "Basic " + TOKEN).statusCode(), path);
            assertEquals(200, send("GET", path, "Authorization", "Bearer " + TOKEN).statusCode(), path);
        }
    }

    @Test
    void thatTheSchemeIsReadWithoutCase() throws Exception {
        assertEquals(200, send("GET", "/ping", "Authorization", "bearer " + TOKEN).statusCode());
    }

    @Test
    void thatCorsIsOffByDefault() throws Exception {
        HttpResponse<String> preflight = send("OPTIONS", "/ping", "Origin", "https://example.org",
                "Access-Control-Request-Method", "GET");
        assertEquals(401, preflight.statusCode());
        assertFalse(preflight.headers().firstValue("Access-Control-Allow-Origin").isPresent());

        HttpResponse<String> response = send("GET", "/ping", "Origin", "https://example.org", "Authorization", "Bearer " + TOKEN);
        assertEquals(200, response.statusCode());
        assertFalse(response.headers().firstValue("Access-Control-Allow-Origin").isPresent());
    }

    @Test
    void thatTheServerDoesNotStartWithoutAToken() {
        DynamicConfiguration configuration = DynamicConfiguration.builder()
                .value("prometheus.defaultExports.disabled", "true")
                .build();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> UndertowApplication.initializeUndertowApplication(configuration, findFree(), null));
        assertTrue(e.getMessage().contains("http.authentication.bearer.token"), e.getMessage());
    }

    @Test
    void thatTheServerDoesNotStartWithAnUnknownProvider() {
        DynamicConfiguration configuration = DynamicConfiguration.builder()
                .value("prometheus.defaultExports.disabled", "true")
                .value("http.authentication.provider", "none")
                .build();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> UndertowApplication.initializeUndertowApplication(configuration, findFree(), null));
        assertEquals("No authentication provider named: none", e.getMessage());
    }

    @Test
    void thatConfiguredCorsAnswersPreflightsBeforeAuthentication() throws Exception {
        DynamicConfiguration configuration = DynamicConfiguration.builder()
                .value("prometheus.defaultExports.disabled", "true")
                .value("http.authentication.bearer.token", TOKEN)
                .value("http.cors.allow.origin", "https://example\\.org")
                .build();
        UndertowApplication corsServer = UndertowApplication.initializeUndertowApplication(configuration, findFree(), null);
        corsServer.start();
        try {
            String base = "http://" + corsServer.getHost() + ":" + corsServer.getPort();
            HttpResponse<String> preflight = client.send(HttpRequest.newBuilder(URI.create(base + "/ping"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .header("Origin", "https://example.org")
                    .header("Access-Control-Request-Method", "GET")
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(204, preflight.statusCode());
            assertEquals("https://example.org", preflight.headers().firstValue("Access-Control-Allow-Origin").orElse(null));

            HttpResponse<String> unauthenticated = client.send(HttpRequest.newBuilder(URI.create(base + "/ping"))
                    .header("Origin", "https://example.org")
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(401, unauthenticated.statusCode());
        } finally {
            corsServer.stop();
        }
    }
}
