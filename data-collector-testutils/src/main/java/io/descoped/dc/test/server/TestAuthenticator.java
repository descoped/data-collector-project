package io.descoped.dc.test.server;

import io.descoped.dc.application.security.BearerTokenAuthenticator;
import io.undertow.server.HttpServerExchange;

/**
 * The test server's authenticator, {@code test}: the bearer token of {@link BearerTokenAuthenticator} for the data
 * collector's endpoints, while the mock source under {@code /api} is open, because it stands in for the services a
 * specification reads, which have their own security.
 */
public class TestAuthenticator extends BearerTokenAuthenticator {

    /**
     * Creates the provider.
     */
    public TestAuthenticator() {
        // the token is read by initialize
    }

    @Override
    public String name() {
        return "test";
    }

    @Override
    public boolean authenticate(HttpServerExchange exchange) {
        String path = exchange.getRequestPath();
        return "/api".equals(path) || path.startsWith("/api/") || super.authenticate(exchange);
    }
}
