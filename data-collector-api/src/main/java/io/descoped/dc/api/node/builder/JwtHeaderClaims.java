package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


// TODO make as builder

/**
 * The header settings of a JWT identity: the signing algorithm and the SSL bundle whose key signs the token and
 * whose certificate chain goes in the {@code x5c} header.
 */
public class JwtHeaderClaims {

    /**
     * Creates an empty instance.
     */
    public JwtHeaderClaims() {
        // the state is in the fields
    }

    @JsonProperty
    Map<String, String> headerClaims = new LinkedHashMap<>();

    /**
     * Sets the {@code alg} header.
     *
     * @param alg the algorithm, for example {@code RS256}
     * @return this instance
     */
    public JwtHeaderClaims alg(String alg) {
        headerClaims.put("alg", alg);
        return this;
    }

    /**
     * Sets the name of the SSL bundle that holds the key pair and certificate chain of the token.
     *
     * @param sslBundleName the SSL bundle's name
     * @return this instance
     */
    public JwtHeaderClaims x509CertChain(String sslBundleName) {
        headerClaims.put("sslBundleName", sslBundleName);
        return this;
    }

    /**
     * The {@code alg} header.
     *
     * @return the algorithm, or {@code null} if not set
     */
    public String alg() {
        return headerClaims.get("alg");
    }

    /**
     * The name of the SSL bundle that holds the key pair and certificate chain of the token.
     *
     * @return the SSL bundle's name, or {@code null} if not set
     */
    public String sslBundleName() {
        return headerClaims.get("sslBundleName");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtHeaderClaims that = (JwtHeaderClaims) o;
        return Objects.equals(headerClaims, that.headerClaims);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headerClaims);
    }

    @Override
    public String toString() {
        return "JwtHeaderClaims{" +
                "headerClaims=" + headerClaims +
                '}';
    }
}
