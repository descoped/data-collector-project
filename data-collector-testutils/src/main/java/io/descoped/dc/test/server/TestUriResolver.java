package io.descoped.dc.test.server;

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
}
