package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The claims of a JWT identity. Values may be expressions, which the engine evaluates when it creates the token.
 */
public class JwtClaims {

    /**
     * Creates an empty instance.
     */
    public JwtClaims() {
        // the state is in the fields
    }

    @JsonProperty("claims")
    Map<String, String> claims = new LinkedHashMap<>();

    /**
     * Sets the {@code issuer} claim.
     *
     * @param issuer the issuer
     * @return this instance
     */
    public JwtClaims issuer(String issuer) {
        claims.put("issuer", issuer);
        return this;
    }

    /**
     * Sets the {@code audience} claim.
     *
     * @param audience the audience
     * @return this instance
     */
    public JwtClaims audience(String audience) {
        claims.put("audience", audience);
        return this;
    }

    /**
     * Sets the token's time to live, which gives its expiry time.
     *
     * @param timeToLiveInSeconds the time to live in seconds
     * @return this instance
     */
    public JwtClaims timeToLiveInSeconds(String timeToLiveInSeconds) {
        claims.put("timeToLiveInSeconds", timeToLiveInSeconds);
        return this;
    }

    /**
     * Sets a claim.
     *
     * @param key   the claim's name
     * @param value the claim's value
     * @return this instance
     */
    public JwtClaims claim(String key, String value) {
        this.claims.put(key, value);
        return this;
    }

    /**
     * The {@code issuer} claim.
     *
     * @return the issuer, or {@code null} if not set
     */
    public String issuer() {
        return claims.get("issuer");
    }

    /**
     * The {@code audience} claim.
     *
     * @return the audience, or {@code null} if not set
     */
    public String audience() {
        return claims.get("audience");
    }

    /**
     * The token's time to live.
     *
     * @return the time to live in seconds, or {@code null} if not set
     */
    public String timeToLiveInSeconds() {
        return claims.get("timeToLiveInSeconds");
    }

    /**
     * All claims, including issuer, audience and time to live, in the order they were set.
     *
     * @return the claims; the map is this instance's own, not a copy
     */
    public Map<String, String> getClaims() {
        return claims;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtClaims claims1 = (JwtClaims) o;
        return Objects.equals(claims, claims1.claims);
    }

    @Override
    public int hashCode() {
        return Objects.hash(claims);
    }

    @Override
    public String toString() {
        return "JwtClaims{" +
                "claims=" + claims +
                '}';
    }
}
