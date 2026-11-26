package io.descoped.dc.api.observer;

/**
 * A write to the content store.
 *
 * @param topic the topic
 */
public record ContentEvent(String topic) {
}
