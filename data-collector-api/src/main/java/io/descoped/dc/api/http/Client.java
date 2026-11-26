package io.descoped.dc.api.http;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.X509TrustManager;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An HTTP client.
 */
public interface Client {

    /**
     * A client with the defaults of the registered {@link Builder}.
     *
     * @return the client
     * @throws java.util.NoSuchElementException if no {@link Builder} is registered
     */
    static Client newClient() {
        return newClientBuilder().build();
    }

    /**
     * A builder from the first registered {@link Builder} service.
     *
     * @return the builder
     * @throws java.util.NoSuchElementException if no {@link Builder} is registered
     */
    static Builder newClientBuilder() {
        return ServiceLoader.load(Client.Builder.class).findFirst().orElseThrow();
    }

    /**
     * The HTTP version.
     *
     * @return the version
     */
    Version version();

    /**
     * Sends a request and reads the body into memory.
     *
     * @param request the request
     * @return the response
     */
    Response send(Request request);

    /**
     * Sends a request and hands the body to a body handler.
     *
     * @param request     the request
     * @param bodyHandler the body handler
     * @param <R>         the body handler's value type
     * @return the response
     */
    <R> Response send(Request request, BodyHandler<R> bodyHandler);

    /**
     * Sends a request asynchronously and reads the body into memory.
     *
     * @param request the request
     * @return the future response
     */
    CompletableFuture<Response> sendAsync(Request request);

    /**
     * Sends a request asynchronously and hands the body to a body handler.
     *
     * @param request     the request
     * @param bodyHandler the body handler
     * @param <R>         the body handler's value type
     * @return the future response
     */
    <R> CompletableFuture<Response> sendAsync(Request request, BodyHandler<R> bodyHandler);

    /**
     * The underlying client.
     *
     * @return the client
     */
    Object getDelegate();

    /**
     * An HTTP version.
     */
    enum Version {
        /**
         * HTTP/1.1.
         */
        HTTP_1_1,
        /**
         * HTTP/2.
         */
        HTTP_2
    }

    /**
     * A redirect policy.
     */
    enum Redirect {

        /**
         * Never redirect.
         */
        NEVER,

        /**
         * Always redirect.
         */
        ALWAYS,

        /**
         * Always redirect, except from HTTPS URLs to HTTP URLs.
         */
        NORMAL
    }

    /**
     * A builder of a {@link Client}; a service the core module provides.
     */
    interface Builder {
        /**
         * Sets the HTTP version.
         *
         * @param version the version
         * @return this builder
         */
        Builder version(Version version);

        /**
         * Sets the HTTP/2 priority of requests.
         *
         * @param priority the priority
         * @return this builder
         */
        Builder priority(int priority);

        /**
         * Sets the authenticator.
         *
         * @param authenticator the authenticator
         * @return this builder
         */
        Builder authenticator(Authenticator authenticator);

        /**
         * Sets the SSL context.
         *
         * @param sslContext the SSL context
         * @return this builder
         */
        Builder sslContext(SSLContext sslContext);

        /**
         * Sets the SSL parameters.
         *
         * @param sslParameters the SSL parameters
         * @return this builder
         */
        Builder sslParameters(SSLParameters sslParameters);

        /**
         * Sets a trust manager; the core module's builder ignores it and trusts what the SSL context trusts.
         *
         * @param trustManager the trust manager
         * @return this builder
         */
        Builder x509TrustManager(X509TrustManager trustManager);

        /**
         * Sets the executor of asynchronous work.
         *
         * @param executor the executor
         * @return this builder
         */
        Builder executor(Executor executor);

        /**
         * Sets the connect timeout.
         *
         * @param duration the timeout
         * @return this builder
         */
        Builder connectTimeout(Duration duration);

        /**
         * Sets the redirect policy.
         *
         * @param policy the policy
         * @return this builder
         */
        Builder followRedirects(Redirect policy);

        /**
         * Sets the proxy selector.
         *
         * @param proxySelector the proxy selector
         * @return this builder
         */
        Builder proxy(ProxySelector proxySelector);

        /**
         * Builds the client.
         *
         * @return the client
         */
        Client build();
    }

}
