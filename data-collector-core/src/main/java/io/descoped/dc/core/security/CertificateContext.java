package io.descoped.dc.core.security;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class CertificateContext {

    final SSLContext sslContext;
    final X509TrustManager trustManager;
    final KeyPair keyPair;
    final X509Certificate[] clientCertificates;

    public CertificateContext(SSLContext sslContext, X509TrustManager trustManager, KeyPair keyPair, X509Certificate... clientCertificates) {
        this.sslContext = sslContext;
        this.trustManager = trustManager;
        this.keyPair = keyPair;
        this.clientCertificates = clientCertificates.clone();
    }

    public SSLContext sslContext() {
        return sslContext;
    }

    public X509TrustManager trustManager() {
        return trustManager;
    }

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
