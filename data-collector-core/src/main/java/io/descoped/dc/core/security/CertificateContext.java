package io.descoped.dc.core.security;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 * A loaded certificate bundle: the SSL context and trust manager for the HTTP client, and the key pair and client
 * certificates for signing JWT grants.
 */
public class CertificateContext {

    final SSLContext sslContext;
    final X509TrustManager trustManager;
    final KeyPair keyPair;
    final X509Certificate[] clientCertificates;

    /**
     * Creates the context; the client certificates are copied.
     *
     * @param sslContext         the SSL context
     * @param trustManager       the trust manager
     * @param keyPair            the bundle's key pair
     * @param clientCertificates the bundle's own certificates
     */
    public CertificateContext(SSLContext sslContext, X509TrustManager trustManager, KeyPair keyPair, X509Certificate... clientCertificates) {
        this.sslContext = sslContext;
        this.trustManager = trustManager;
        this.keyPair = keyPair;
        this.clientCertificates = clientCertificates.clone();
    }

    /**
     * The SSL context.
     *
     * @return the SSL context
     */
    public SSLContext sslContext() {
        return sslContext;
    }

    /**
     * The trust manager.
     *
     * @return the trust manager
     */
    public X509TrustManager trustManager() {
        return trustManager;
    }

    /**
     * The bundle's key pair.
     *
     * @return the key pair
     */
    public KeyPair keyPair() {
        return keyPair;
    }

    /**
     * The bundle's own certificate, as a JWT grant sends it in its {@code x5c} header. The trust manager's accepted
     * issuers are the trust anchors ({@code cacerts} and the bundle's), not this certificate.
     *
     * @return a copy of the certificates
     */
    public X509Certificate[] clientCertificates() {
        return clientCertificates.clone();
    }
}
