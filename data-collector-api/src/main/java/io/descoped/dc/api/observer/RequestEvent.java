package io.descoped.dc.api.observer;

/**
 * An HTTP request.
 *
 * @param location the scheme, host and port of its URL, without path or query; empty when the URL is not known
 */
public record RequestEvent(String location) {
}
