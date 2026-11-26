package io.descoped.dc.core.security;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

/**
 * The trust manager of a certificate bundle: the JDK's PKIX trust manager over its {@code cacerts}, and the bundle's
 * own trust anchors (CA certificates) when it has some. A server's chain is always verified up to a trust anchor; it
 * is never trusted by comparing certificates.
 */
final class TrustManagers {

    private TrustManagers() {
    }

    /**
     * Creates the trust manager.
     *
     * @param trustCertificates the bundle's CA certificates in PEM, one or more; {@code null} or empty for the JDK's
     *                          {@code cacerts} only
     * @return the trust manager
     * @throws IllegalStateException if the CA certificates cannot be read
     */
    static X509TrustManager create(char[] trustCertificates) {
        try {
            X509TrustManager jdk = trustManager(null);
            if (trustCertificates == null || trustCertificates.length == 0) {
                return jdk;
            }
            KeyStore anchors = KeyStore.getInstance(KeyStore.getDefaultType());
            anchors.load(null, null);
            int n = 0;
            for (X509Certificate issuer : jdk.getAcceptedIssuers()) {
                anchors.setCertificateEntry("cacerts-" + n++, issuer);
            }
            Collection<? extends Certificate> certificates = parse(trustCertificates);
            if (certificates.isEmpty()) {
                throw new IllegalStateException("The bundle's trust certificates hold no certificate");
            }
            for (Certificate certificate : certificates) {
                anchors.setCertificateEntry("bundle-" + n++, certificate);
            }
            return trustManager(anchors);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Could not create the trust manager of the bundle", e);
        }
    }

    private static Collection<? extends Certificate> parse(char[] pem) throws GeneralSecurityException, IOException {
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(pem));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            return CertificateFactory.getInstance("X.509").generateCertificates(in);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    private static X509TrustManager trustManager(KeyStore anchors) throws GeneralSecurityException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(anchors);
        for (TrustManager trustManager : factory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new IllegalStateException("No X509TrustManager found");
    }
}
