package io.descoped.dc.api.node;

import io.descoped.dc.api.node.builder.JwtClaims;
import io.descoped.dc.api.node.builder.JwtHeaderClaims;

/**
 * An identity that signs a JWT.
 */
public interface JwtIdentity extends Identity {

    /**
     * The claims of the JWT's header.
     *
     * @return the header claims
     */
    JwtHeaderClaims headerClaims();

    /**
     * The claims of the JWT's body.
     *
     * @return the claims
     */
    JwtClaims claims();

}
