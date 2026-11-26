package io.descoped.dc.api.content;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.descoped.dc.api.CorrelationIds;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.util.JsonParser;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The metadata of a piece of content, as a JSON object: metadata, HTTP info with request and response headers,
 * and the state.
 */
public class MetadataContent {

    private final ObjectNode elementNode;

    /**
     * Creates the metadata on a JSON object.
     *
     * @param elementNode the object
     */
    public MetadataContent(ObjectNode elementNode) {
        this.elementNode = elementNode;
    }

    /**
     * The JSON object.
     *
     * @return the object
     */
    public ObjectNode getElementNode() {
        return elementNode;
    }

    /**
     * The JSON object as text.
     *
     * @return the JSON
     */
    public String toJSON() {
        return JsonParser.createJsonParser().toJSON(elementNode);
    }

    /**
     * The kind of content.
     */
    public enum ResourceType {
        /**
         * A page document.
         */
        PAGE,
        /**
         * A page entry.
         */
        ENTRY,
        /**
         * A document fetched for an entry.
         */
        DOCUMENT
    }

    /**
     * A builder of {@link MetadataContent}.
     */
    public static class Builder {

        private final ObjectNode metadataNode = JsonParser.createJsonParser().createObjectNode();
        private final ObjectNode requestHeaderNode = JsonParser.createJsonParser().createObjectNode();
        private final ObjectNode responseHeaderNode = JsonParser.createJsonParser().createObjectNode();
        private final ObjectNode httpInfoNode = JsonParser.createJsonParser().createObjectNode();
        private final ObjectNode stateNode = JsonParser.createJsonParser().createObjectNode();

        /**
         * Creates an empty builder.
         */
        public Builder() {
            // the state is in the fields
        }

        /**
         * Sets the correlation ids, comma separated.
         *
         * @param correlationIds the correlation ids, or {@code null}
         * @return this builder
         */
        public Builder correlationId(CorrelationIds correlationIds) {
            metadataNode.put("correlation-id", (correlationIds == null ? null : correlationIds.get().stream().map(UUID::toString).collect(Collectors.joining(","))));
            return this;
        }

        /**
         * Sets the topic.
         *
         * @param topic the topic
         * @return this builder
         */
        public Builder topic(String topic) {
            metadataNode.put("topic", topic);
            return this;
        }

        /**
         * Sets the position.
         *
         * @param position the position
         * @return this builder
         */
        public Builder position(String position) {
            metadataNode.put("position", position);
            return this;
        }

        /**
         * Sets the content key.
         *
         * @param contentKey the content key
         * @return this builder
         */
        public Builder contentKey(String contentKey) {
            metadataNode.put("content-key", contentKey);
            return this;
        }

        /**
         * Sets the kind of content.
         *
         * @param resourceType the kind
         * @return this builder
         */
        public Builder resourceType(ResourceType resourceType) {
            metadataNode.put("resource-type", resourceType.name().toLowerCase());
            return this;
        }

        /**
         * Sets the content type.
         *
         * @param contentType the content type
         * @return this builder
         */
        public Builder contentType(String contentType) {
            metadataNode.put("content-type", contentType);
            return this;
        }

        /**
         * Sets the content length.
         *
         * @param contentLength the length, in bytes
         * @return this builder
         */
        public Builder contentLength(int contentLength) {
            metadataNode.put("content-length", contentLength);
            return this;
        }

        /**
         * Sets the request's duration.
         *
         * @param requestDurationNanoTime the duration, in nanoseconds
         * @return this builder
         */
        public Builder requestDurationNanoTime(long requestDurationNanoTime) {
            metadataNode.put("request-duration-nano-time", requestDurationNanoTime);
            return this;
        }

        /**
         * Sets the URL.
         *
         * @param url the URL
         * @return this builder
         */
        public Builder url(String url) {
            metadataNode.put("url", url);
            return this;
        }

        /**
         * Sets the response status code.
         *
         * @param statusCode the status code
         * @return this builder
         */
        public Builder statusCode(int statusCode) {
            httpInfoNode.put("statusCode", statusCode);
            return this;
        }

        /**
         * Adds the request headers, each header's values comma separated.
         *
         * @param requestHeaders the headers
         * @return this builder
         */
        public Builder requestHeaders(Headers requestHeaders) {
            requestHeaders.asMap().forEach((key, value) -> requestHeaderNode.put(key, String.join(",", value)));
            return this;
        }

        /**
         * Adds the response headers, each header's values comma separated.
         *
         * @param responseHeaders the headers
         * @return this builder
         */
        public Builder responseHeaders(Headers responseHeaders) {
            responseHeaders.asMap().forEach((key, value) -> responseHeaderNode.put(key, String.join(",", value)));
            return this;
        }

        /**
         * Adds state values.
         *
         * @param stateMap the values, strings, integers or booleans; {@code null} adds nothing
         * @return this builder
         * @throws IllegalStateException if a value has another type
         */
        public Builder state(Map<String, Object> stateMap) {
            if (stateMap == null) {
                return this;
            }
            stateMap.entrySet().forEach(entry -> {
                Object value = entry.getValue();
                ValueNode valueNode;
                if (value instanceof String string) {
                    valueNode = new TextNode(string);

                } else if (value instanceof Integer integer) {
                    valueNode = new IntNode(integer);

                } else if (value instanceof Boolean bool) {
                    valueNode = BooleanNode.valueOf(bool);

                } else {
                    throw new IllegalStateException("Unsupported state type!");
                }
                stateNode.set(entry.getKey(), valueNode);
            });
            return this;
        }

        /**
         * Builds the metadata; the state is left out when it is empty.
         *
         * @return the metadata
         */
        public MetadataContent build() {
            JsonParser jsonParser = JsonParser.createJsonParser();
            ObjectNode elementNode = jsonParser.createObjectNode();
            elementNode.set("metadata", metadataNode);
            httpInfoNode.set("request-headers", requestHeaderNode);
            httpInfoNode.set("response-headers", responseHeaderNode);
            elementNode.set("http-info", httpInfoNode);
            if (!stateNode.isEmpty()) {
                elementNode.set("state", stateNode);
            }
            return new MetadataContent(elementNode);
        }
    }
}
