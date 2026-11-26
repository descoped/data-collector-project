package io.descoped.dc.core.security;

import io.descoped.dc.api.security.BusinessSSLResource;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static io.descoped.dc.core.security.TrustManagersTest.certificate;
import static io.descoped.dc.core.security.TrustManagersTest.keyPair;
import static io.descoped.dc.core.security.TrustManagersTest.pem;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A PEM bundle given as a {@link BusinessSSLResource}: the context's client certificates are the bundle's own (what
 * a JWT grant sends as x5c), and the bundle's trust anchors reach its trust manager.
 */
class CertificateContextTest {

    static BusinessSSLResource bundle(X509Certificate certificate, KeyPair keys, char[] trustCertificates) throws Exception {
        char[] publicCertificate = pem(certificate);
        char[] privateKey = ("-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(keys.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n").toCharArray();
        return new BusinessSSLResource() {
            @Override
            public String bundleName() {
                return "test-bundle";
            }

            @Override
            public String getType() {
                return "pem";
            }

            @Override
            public char[] publicCertificate() {
                return publicCertificate.clone();
            }

            @Override
            public char[] privateCertificate() {
                return privateKey.clone();
            }

            @Override
            public byte[] archiveCertificate() {
                return new byte[0];
            }

            @Override
            public char[] passphrase() {
                return new char[0];
            }

            @Override
            public char[] trustCertificates() {
                return trustCertificates == null ? new char[0] : trustCertificates.clone();
            }

            @Override
            public void close() {
                // nothing to release: the bundle is held in memory
            }
        };
    }

    @Test
    void thatTheClientCertificatesAreTheBundlesOwn() throws Exception {
        KeyPair clientKeys = keyPair();
        X509Certificate client = certificate("CN=business-client", clientKeys, "CN=business-client", clientKeys, false);
        CertificateContext context = CertificateFactory.create(bundle(client, clientKeys, null)).getCertificateContext("test-bundle");
        assertArrayEquals(new X509Certificate[]{client}, context.clientCertificates());
    }

    @Test
    void thatTheBundlesTrustAnchorsReachItsTrustManager() throws Exception {
        KeyPair clientKeys = keyPair();
        X509Certificate client = certificate("CN=business-client", clientKeys, "CN=business-client", clientKeys, false);
        KeyPair caKeys = keyPair();
        X509Certificate ca = certificate("CN=private-ca", caKeys, "CN=private-ca", caKeys, true);
        KeyPair serverKeys = keyPair();
        X509Certificate server = certificate("CN=api.example.org", serverKeys, "CN=private-ca", caKeys, false);

        CertificateContext without = CertificateFactory.create(bundle(client, clientKeys, null)).getCertificateContext("test-bundle");
        assertThrows(CertificateException.class, () -> without.trustManager().checkServerTrusted(new X509Certificate[]{server}, "RSA"));

        CertificateContext with = CertificateFactory.create(bundle(client, clientKeys, pem(ca))).getCertificateContext("test-bundle");
        assertDoesNotThrow(() -> with.trustManager().checkServerTrusted(new X509Certificate[]{server}, "RSA"));
        assertArrayEquals(new X509Certificate[]{client}, with.clientCertificates());
    }
}
