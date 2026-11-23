package io.descoped.dc.api.content;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.http.Headers;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataContentTest {

    @Test
    void thatMetadataHttpInfoAndStateAreBuilt() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("text", "a");
        state.put("number", 1);
        state.put("flag", true);

        ObjectNode element = new MetadataContent.Builder()
                .topic("topic")
                .position("1")
                .resourceType(MetadataContent.ResourceType.ENTRY)
                .statusCode(200)
                .requestHeaders(new Headers(Map.of("Accept", List.of("a", "b"))))
                .responseHeaders(new Headers(Map.of("Content-Type", List.of("application/json"))))
                .state(state)
                .build()
                .getElementNode();

        assertEquals("topic", element.get("metadata").get("topic").textValue());
        assertEquals("1", element.get("metadata").get("position").textValue());
        assertEquals("entry", element.get("metadata").get("resource-type").textValue());
        assertEquals(200, element.get("http-info").get("statusCode").intValue());
        assertEquals("a,b", element.get("http-info").get("request-headers").get("Accept").textValue());
        assertEquals("application/json", element.get("http-info").get("response-headers").get("Content-Type").textValue());
        assertEquals("a", element.get("state").get("text").textValue());
        assertEquals(1, element.get("state").get("number").intValue());
        assertTrue(element.get("state").get("flag").booleanValue());
    }

    @Test
    void thatNoStateLeavesTheStateOut() {
        ObjectNode element = new MetadataContent.Builder().state(null).build().getElementNode();
        assertFalse(element.has("state"));
    }

    @Test
    void thatAnUnsupportedStateTypeIsRefused() {
        MetadataContent.Builder builder = new MetadataContent.Builder();
        Map<String, Object> state = Map.of("decimal", 1.5d);
        assertThrows(IllegalStateException.class, () -> builder.state(state));
    }
}
