package io.descoped.dc.api.node;

/**
 * A producer of a request body made from a JWT signed by an identity.
 */
public interface JwtTokenBodyPublisherProducer extends IdentityTokenBodyPublisherProducer {

    /**
     * The identity that signs the JWT.
     *
     * @return the identity
     */
    Identity identity();

    /**
     * The name of the variable the signed JWT is bound to when {@link #token()} is evaluated.
     *
     * @return the variable name
     */
    String bindTo();

    /**
     * The expression that gives the body.
     *
     * @return the expression
     */
    String token();
}
