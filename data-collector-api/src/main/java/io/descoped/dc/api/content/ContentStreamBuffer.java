package io.descoped.dc.api.content;

import io.descoped.dc.api.ids.TimeOrderedIds;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A message of the content stream: a position and its content by key, with a manifest of metadata.
 */
public interface ContentStreamBuffer {

    /**
     * The message id: a UUIDv7, ordered by {@link TimeOrderedIds#ORDER}; null before the message is published.
     *
     * @return the id
     */
    UUID id();

    /**
     * The time the message id holds.
     *
     * @return the timestamp, in milliseconds since the epoch
     */
    default long timestamp() {
        return TimeOrderedIds.timestamp(id());
    }

    /**
     * The message's position.
     *
     * @return the position
     */
    String position();

    /**
     * The content keys.
     *
     * @return the keys
     */
    Set<String> keys();

    /**
     * The content under a key.
     *
     * @param contentKey the key
     * @return the content, or {@code null} if there is none
     */
    byte[] get(String contentKey);

    /**
     * The content, by key.
     *
     * @return the content
     */
    Map<String, byte[]> data();

    /**
     * The metadata of the content.
     *
     * @return the manifest
     */
    List<MetadataContent> manifest();

    /**
     * A builder of a {@link ContentStreamBuffer}.
     */
    interface Builder {

        /**
         * Sets the message id.
         *
         * @param id the id
         * @return this builder
         */
        Builder id(UUID id);

        /**
         * Sets the position.
         *
         * @param position the position
         * @return this builder
         */
        Builder position(String position);

        /**
         * The position set.
         *
         * @return the position, or {@code null} if none is set
         */
        String position();

        /**
         * Puts content under a key, without metadata.
         *
         * @param key     the key
         * @param payload the content
         * @return this builder
         */
        ContentStreamBuffer.Builder put(String key, byte[] payload);

        /**
         * Puts content under a key and adds its metadata to the manifest.
         *
         * @param contentKey the key
         * @param content    the content
         * @param manifest   the metadata, or {@code null} for none
         * @return this builder
         */
        Builder buffer(String contentKey, byte[] content, MetadataContent manifest);

        /**
         * The content under a key.
         *
         * @param contentKey the key
         * @return the content, or {@code null} if there is none
         */
        byte[] get(String contentKey);

        /**
         * The content keys.
         *
         * @return the keys
         */
        Set<String> keys();

        /**
         * The metadata added so far.
         *
         * @return the manifest
         */
        List<MetadataContent> manifest();

        /**
         * Builds the message.
         *
         * @return the message
         */
        ContentStreamBuffer build();
    }

}
