package io.descoped.dc.core.http;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

/**
 * The location and path of a URL, for the metrics labels.
 */
public class URLInfo {
    private final URL url;

    /**
     * Parses the URL.
     *
     * @param url the URL, or {@code null}
     * @throws IllegalArgumentException when the URL is not a valid URI
     * @throws RuntimeException         when the URI cannot be read as a URL
     */
    public URLInfo(String url) {
        try {
            // parsed as the request was sent (HttpRequestDelegate: URI.create), then read as a URL as before
            this.url = (url != null ? URI.create(url).toURL() : null);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The URL's scheme, host and port; the port is left out when it is absent, 80 or 443.
     *
     * @return the location, or empty when the URL is {@code null}
     */
    public Optional<String> getLocation() {
        if (url == null) {
            return Optional.empty();
        }
        return Optional.of(String.format("%s://%s%s", url.getProtocol(), url.getHost(), (-1 == url.getPort() || 80 == url.getPort() || 443 == url.getPort() ? "" : ":" + url.getPort())));
    }

    /**
     * The URL's path.
     *
     * @return the path, or empty when the URL is {@code null}
     */
    public Optional<String> getRequestPath() {
        if (url == null) {
            return Optional.empty();
        }
        return Optional.of(url.getPath());
    }
}
