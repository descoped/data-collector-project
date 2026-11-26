package io.descoped.dc.api.http;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * An HTTP response.
 */
public interface Response {

    /**
     * A builder from the first registered {@link Builder} service.
     *
     * @return the builder
     * @throws java.util.NoSuchElementException if no {@link Builder} is registered
     */
    static Builder newResponseBuilder() {
        return ServiceLoader.load(Response.Builder.class).findFirst().orElseThrow();
    }

    /**
     * The URL of the request.
     *
     * @return the URL
     */
    String url();

    /**
     * The headers.
     *
     * @return the headers
     */
    Headers headers();

    /**
     * The status code.
     *
     * @return the status code
     */
    int statusCode();

    /**
     * The body read into memory; empty when a body handler received it.
     *
     * @return the body
     */
    byte[] body();

    /**
     * The body handler that received this response's body, typed by the caller: {@code response.<TYPE>bodyHandler()}.
     *
     * @param <R> the body type the handler produces
     * @return the body handler, or empty when the body was read into memory
     */
    <R> Optional<BodyHandler<R>> bodyHandler();

    /**
     * The response this one follows, as after a redirect.
     *
     * @return the previous response, or empty if there is none
     */
    Optional<Response> previousResponse();

    /**
     * A builder of a {@link Response}; a service the core module provides.
     */
    interface Builder {
        /**
         * Sets the underlying response.
         *
         * @param delegate the response
         * @return this builder
         */
        Builder delegate(Object delegate);

        /**
         * Sets the body handler that received the body.
         *
         * @param bodyHandler the body handler
         * @param <R>         the body handler's value type
         */
        <R> void bodyHandler(BodyHandler<R> bodyHandler);

        /**
         * Builds the response.
         *
         * @return the response
         */
        Response build();
    }

}
