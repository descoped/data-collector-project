package io.descoped.dc.application.spi;

import io.descoped.config.DynamicConfiguration;
import io.undertow.server.HttpServerExchange;

/**
 * Authenticates the requests to the server's endpoints. Every request passes the authenticator before it reaches a
 * controller; a request it refuses gets {@code 401 Unauthorized}.
 *
 * <p>An authenticator is a service provider: it is registered with {@code provides} in its {@code module-info.java}
 * and in {@code META-INF/services}, and it has a public constructor without parameters. The server uses the one whose
 * {@link #name()} is the value of {@code http.authentication.provider} ({@code bearer}, the built-in bearer token,
 * when the key is not set). The server does not start when no provider has that name, or when the chosen provider
 * finds its configuration incomplete.
 */
public interface Authenticator {

    /**
     * The name the configuration chooses this provider by.
     *
     * @return the name
     */
    String name();

    /**
     * Reads the provider's configuration. Called once, before the server starts.
     *
     * @param configuration the server's configuration
     * @throws IllegalStateException if the configuration the provider needs is missing or invalid, so the server does
     *                               not start without authentication
     */
    void initialize(DynamicConfiguration configuration);

    /**
     * Whether a request is authenticated. It is called on a worker thread, so it may block. It never logs or returns
     * the credentials it reads.
     *
     * @param exchange the request
     * @return true if the request may reach the endpoints
     */
    boolean authenticate(HttpServerExchange exchange);

    /**
     * The {@code WWW-Authenticate} value sent with a {@code 401}.
     *
     * @return the challenge
     */
    String challenge();
}
