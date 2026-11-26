package io.descoped.dc.api.security;

import java.util.Objects;

public interface BusinessSSLResource extends AutoCloseable {

    String bundleName();

    String getType();

    char[] publicCertificate();

    char[] privateCertificate();

    byte[] archiveCertificate();

    char[] passphrase();

    /**
     * The bundle's trust anchors: CA certificates in PEM, added to the JDK's {@code cacerts} to verify the servers the
     * bundle is used with, whose chains are not in {@code cacerts}.
     *
     * @return the certificates, or {@code null} or empty for {@code cacerts} only
     */
    default char[] trustCertificates() {
        return null;
    }

    @Override
    void close();

    default boolean isPEM() {
        Objects.requireNonNull(getType());
        return "pem".equalsIgnoreCase(getType());
    }
}
