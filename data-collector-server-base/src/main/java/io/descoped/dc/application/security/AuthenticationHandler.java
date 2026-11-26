package io.descoped.dc.application.security;

import io.descoped.dc.application.spi.Authenticator;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.util.Objects;

/**
 * Lets a request through to the next handler only when the authenticator accepts it; otherwise it answers
 * {@code 401 Unauthorized} with the authenticator's challenge.
 */
public class AuthenticationHandler implements HttpHandler {

    private final Authenticator authenticator;
    private final HttpHandler next;

    /**
     * Creates the handler.
     *
     * @param authenticator the authenticator, initialized
     * @param next          the handler of an authenticated request
     */
    public AuthenticationHandler(Authenticator authenticator, HttpHandler next) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.next = Objects.requireNonNull(next, "next");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // an authenticator may block (a key set, an introspection call), so it runs on a worker thread
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        if (authenticator.authenticate(exchange)) {
            next.handleRequest(exchange);
            return;
        }
        exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
        exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, authenticator.challenge());
        exchange.endExchange();
    }
}
