package io.descoped.dc.core.http;

import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.core.metrics.MetricsAgent;
import io.prometheus.client.CollectorRegistry;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The metrics agent records the client's requests (issue #25: no dc_client metric was recorded).
 */
class HttpClientExporterTest {

    static ServerSocket server;

    @BeforeAll
    static void beforeAll() throws IOException {
        MetricsAgent.premain(null, ByteBuddyAgent.install());
        server = new ServerSocket(0);
        Thread.ofVirtual().start(() -> {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    socket.getInputStream().read(new byte[8192]);
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".getBytes(StandardCharsets.US_ASCII));
                } catch (IOException e) {
                    // the server is closed
                }
            }
        });
    }

    @AfterAll
    static void afterAll() throws IOException {
        server.close();
    }

    static double completed(String location) {
        Double value = CollectorRegistry.defaultRegistry.getSampleValue("dc_client_request_completed_total",
                new String[]{"location", "statusCode", "emptyResponse"}, new String[]{location, "200", "false"});
        return value == null ? 0 : value;
    }

    @Test
    void thatSendAndSendAsyncAreCounted() throws Exception {
        String url = "http://127.0.0.1:" + server.getLocalPort() + "/metrics-test";
        String location = new URLInfo(url).getLocation().orElse("");
        double before = completed(location);

        Client client = Client.newClient();
        assertNotNull(client.send(Request.newRequestBuilder().GET().url(url).build()));
        assertNotNull(client.sendAsync(Request.newRequestBuilder().GET().url(url).build()).get());

        // the asynchronous response is counted when its future completes, which may be just after get() returns
        for (int i = 0; i < 50 && completed(location) < before + 2; i++) {
            Thread.sleep(20);
        }
        assertEquals(before + 2, completed(location));
    }
}
