package io.descoped.dc.api.http;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.Flow;

/**
 * An HTTP request.
 */
public interface Request {

    /**
     * A builder from the first registered {@link Builder} service.
     *
     * @return the builder
     * @throws java.util.NoSuchElementException if no {@link Builder} is registered
     */
    static Builder newRequestBuilder() {
        return ServiceLoader.load(Request.Builder.class).findFirst().orElseThrow();
    }

    /**
     * The URL.
     *
     * @return the URL
     */
    String url();

    /**
     * The method.
     *
     * @return the method
     */
    Method method();

    /**
     * The headers.
     *
     * @return the headers
     */
    Headers headers();

    /**
     * The underlying request.
     *
     * @return the request
     */
    Object getDelegate();

    /**
     * An HTTP method.
     */
    enum Method {
        /**
         * PUT.
         */
        PUT,
        /**
         * POST.
         */
        POST,
        /**
         * GET.
         */
        GET,
        /**
         * DELETE.
         */
        DELETE,
        /**
         * OPTIONS.
         */
        OPTIONS;
    }

    /**
     * A builder of a {@link Request}; a service the core module provides.
     */
    interface Builder {
        /**
         * Sets the URL.
         *
         * @param url the URL
         * @return this builder
         */
        Builder url(String url);

        /**
         * Makes a PUT request with a body.
         *
         * @param bytes the body
         * @return this builder
         */
        Builder PUT(byte[] bytes);

        /**
         * Makes a PUT request with a body.
         *
         * @param bodyPublisher the body
         * @return this builder
         */
        Builder PUT(Flow.Publisher<ByteBuffer> bodyPublisher);

        /**
         * Makes a POST request with a body.
         *
         * @param bytes the body
         * @return this builder
         */
        Builder POST(byte[] bytes);

        /**
         * Makes a POST request with a body.
         *
         * @param bodyPublisher the body
         * @return this builder
         */
        Builder POST(Flow.Publisher<ByteBuffer> bodyPublisher);

        /**
         * Makes a GET request.
         *
         * @return this builder
         */
        Builder GET();

        /**
         * Makes a DELETE request.
         *
         * @return this builder
         */
        Builder DELETE();

        /**
         * Adds a header value.
         *
         * @param name  the name
         * @param value the value
         * @return this builder
         */
        Builder header(String name, String value);

        /**
         * Sets whether the request asks for {@code 100 Continue}.
         *
         * @param enable {@code true} to ask
         * @return this builder
         */
        Builder expectContinue(boolean enable);

        /**
         * Sets the request timeout.
         *
         * @param duration the timeout
         * @return this builder
         */
        Builder timeout(Duration duration);

        /**
         * Builds the request.
         *
         * @return the request
         */
        Request build();
    }

}
