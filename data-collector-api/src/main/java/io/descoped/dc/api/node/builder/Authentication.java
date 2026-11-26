package io.descoped.dc.api.node.builder;

/**
 * Kinds of authentication. No code in the data collector reads these values.
 */
public enum Authentication {
    /** No authentication. */
    NONE,
    /** HTTP basic authentication. */
    BASIC,
    /** HTTP digest authentication. */
    DIGEST,
    /** OAuth 2.0. */
    OAUTH2,
    /** Client authentication through an SSL socket factory. */
    SSL_FACTORY
}
