package io.descoped.dc.application.security;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.application.spi.Authenticator;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The built-in authenticator, {@code bearer}: a request is authenticated when its {@code Authorization} header is
 * {@code Bearer <token>} with the token in {@code http.authentication.bearer.token}. The tokens are compared by their
 * SHA-256 digests in constant time.
 */
public class BearerTokenAuthenticator implements Authenticator {

    /**
     * The configuration key of the token.
     */
    public static final String TOKEN_KEY = "http.authentication.bearer.token";

    private static final String SCHEME = "Bearer ";

    private byte[] tokenDigest;

    /**
     * Creates the provider; {@link #initialize(DynamicConfiguration)} reads the token.
     */
    public BearerTokenAuthenticator() {
        // the token is read by initialize
    }

    @Override
    public String name() {
        return "bearer";
    }

    @Override
    public void initialize(DynamicConfiguration configuration) {
        String token = configuration.find(TOKEN_KEY).filter(value -> !value.isBlank()).orElseThrow(() ->
                new IllegalStateException(TOKEN_KEY + " is not set: every endpoint requires authentication"));
        tokenDigest = digest(token);
    }

    @Override
    public boolean authenticate(HttpServerExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        if (tokenDigest == null || authorization == null
                || !authorization.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            return false;
        }
        return MessageDigest.isEqual(digest(authorization.substring(SCHEME.length()).trim()), tokenDigest);
    }

    @Override
    public String challenge() {
        return "Bearer realm=\"data-collector\"";
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
