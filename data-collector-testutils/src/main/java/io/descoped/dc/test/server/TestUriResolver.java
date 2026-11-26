package io.descoped.dc.test.server;

import java.util.Optional;

/**
 * Resolves a path to a URL on the test server.
 */
public interface TestUriResolver {

    /**
     * The URL of a path on the test server.
     *
     * @param uri the path
     * @return the URL
     */
    String testURL(String uri);

    /**
     * The {@code Authorization} header a client sends to the server, if it needs one.
     *
     * @return the header's value, or empty
     */
    default Optional<String> authorization() {
        return Optional.empty();
    }
}
