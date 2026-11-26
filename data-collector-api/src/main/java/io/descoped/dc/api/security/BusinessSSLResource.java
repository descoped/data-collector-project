package io.descoped.dc.api.security;

import java.util.Objects;

/**
 * The secrets of a business certificate bundle: PEM certificate and key, or a PKCS#12 archive, with its
 * passphrase. It is closed once the bundle is loaded, so an implementation can clear the secrets.
 */
public interface BusinessSSLResource extends AutoCloseable {

    /**
     * The bundle's name.
     *
     * @return the name
     */
    String bundleName();

    /**
     * The bundle's type: {@code pem} for PEM, anything else for an archive.
     *
     * @return the type
     */
    String getType();

    /**
     * The certificate, in PEM; read for a PEM bundle.
     *
     * @return the certificate
     */
    char[] publicCertificate();

    /**
     * The private key, in PEM; read for a PEM bundle.
     *
     * @return the private key
     */
    char[] privateCertificate();

    /**
     * The PKCS#12 archive; read for a bundle that is not PEM.
     *
     * @return the archive
     */
    byte[] archiveCertificate();

    /**
     * The passphrase of the key or the archive.
     *
     * @return the passphrase
     */
    char[] passphrase();

    /**
     * The bundle's trust anchors: CA certificates in PEM, added to the JDK's {@code cacerts} to verify the servers the
     * bundle is used with, whose chains are not in {@code cacerts}.
     *
     * @return the certificates, or empty for {@code cacerts} only
     */
    default char[] trustCertificates() {
        return new char[0];
    }

    /**
     * Closes the resource, without a checked exception.
     */
    @Override
    void close();

    /**
     * Whether the bundle is PEM: its type is {@code pem}, in any case.
     *
     * @return {@code true} for PEM
     * @throws NullPointerException if the type is {@code null}
     */
    default boolean isPEM() {
        Objects.requireNonNull(getType());
        return "pem".equalsIgnoreCase(getType());
    }
}
