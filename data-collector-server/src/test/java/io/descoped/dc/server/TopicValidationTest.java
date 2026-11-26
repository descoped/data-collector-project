package io.descoped.dc.server;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.server.db.SequenceDbHelper;
import io.descoped.dc.test.client.TestClient;
import io.descoped.dc.test.server.TestServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Topic names from requests are rawdata topic names, checked before they reach a path.
 */
class TopicValidationTest {

    static TestServer server;
    static TestClient client;

    @BeforeAll
    static void beforeAll() {
        DynamicConfiguration configuration = DynamicConfiguration.builder()
                .value("content.stream.connector", "rawdata")
                .value("rawdata.client.provider", "memory")
                .value("data.collector.integrityCheck.consumer.timeoutInSeconds", "1")
                .value("data.collector.integrityCheck.database.location", "./target/topic-validation")
                .build();
        server = TestServer.create(configuration);
        client = TestClient.create(server);
        server.start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void thatIntegrityCheckRefusesTopicsThatAreNotTopicNames() throws Exception {
        // a file next to the integrity indexes, which a topic of ".." would have deleted with its directory
        Path marker = Path.of("target", "topic-validation", "marker");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "kept");

        for (String topic : new String[]{"%2E%2E", "%2E", ".hidden", "a%7Cb", "%7B..%7D"}) {
            assertEquals(400, client.put("/check-integrity/" + topic).response().statusCode(), topic);
            assertEquals(400, client.get("/check-integrity/" + topic).response().statusCode(), topic);
            assertEquals(400, client.get("/check-integrity/" + topic + "/full").response().statusCode(), topic);
            assertEquals(400, client.delete("/check-integrity/" + topic).response().statusCode(), topic);
        }
        assertTrue(Files.exists(marker));
    }

    @Test
    void thatRecoveryRefusesTopicsThatAreNotTopicNames() {
        assertEquals(400, client.put("/recovery/%7B..%2F..%2Ftmp%2Fx%7D?toTopic=target").response().statusCode());
        assertEquals(400, client.put("/recovery/%2E%2E?toTopic=target").response().statusCode());
        assertEquals(400, client.put("/recovery/source?toTopic=..%2F..%2Fx").response().statusCode());
        assertEquals(400, client.get("/recovery/%2E%2E").response().statusCode());
        assertEquals(400, client.delete("/recovery/%2E%2E").response().statusCode());
    }

    @Test
    void thatTopicDirectoriesStayInTheirLocation() {
        Path location = Path.of("lmdb");
        assertEquals(location.resolve("2020-test.stream_1"), SequenceDbHelper.topicDirectory(location, "2020-test.stream_1"));
        for (String topic : new String[]{"..", ".", "", "a/b", "a\\b", "/abs", "{x}", "a b"}) {
            assertThrows(IllegalArgumentException.class, () -> SequenceDbHelper.topicDirectory(location, topic), topic);
        }
        assertThrows(NullPointerException.class, () -> SequenceDbHelper.topicDirectory(location, null));
    }
}
