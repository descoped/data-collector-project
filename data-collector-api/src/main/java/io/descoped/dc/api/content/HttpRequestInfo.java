package io.descoped.dc.api.content;

import io.descoped.dc.api.CorrelationIds;
import io.descoped.dc.api.http.Headers;

/**
 * What the content store records about the request that fetched content.
 */
public class HttpRequestInfo {

    private final CorrelationIds correlationIds;
    private final String url;
    private final Headers requestHeaders;
    private final Headers responseHeaders;
    private final long requestDurationNanoSeconds;
    private final int statusCode;

    /**
     * Creates the record.
     *
     * @param correlationIds             the correlation ids
     * @param url                        the URL
     * @param statusCode                 the response status code
     * @param requestHeaders             the request headers
     * @param responseHeaders            the response headers
     * @param requestDurationNanoSeconds the request's duration, in nanoseconds
     */
    public HttpRequestInfo(CorrelationIds correlationIds, String url, int statusCode, Headers requestHeaders, Headers responseHeaders, long requestDurationNanoSeconds) {
        this.correlationIds = correlationIds;
        this.url = url;
        this.statusCode = statusCode;
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
        this.requestDurationNanoSeconds = requestDurationNanoSeconds;
    }

    /**
     * The correlation ids.
     *
     * @return the correlation ids
     */
    public CorrelationIds getCorrelationIds() {
        return correlationIds;
    }

    /**
     * The URL.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * The request headers.
     *
     * @return the headers
     */
    public Headers getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * The response headers.
     *
     * @return the headers
     */
    public Headers getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * The request's duration.
     *
     * @return the duration, in nanoseconds
     */
    public long getRequestDurationNanoSeconds() {
        return requestDurationNanoSeconds;
    }

    /**
     * The response status code.
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

}
