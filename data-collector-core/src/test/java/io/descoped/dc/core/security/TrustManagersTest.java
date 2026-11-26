package io.descoped.dc.core.security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import javax.net.ssl.X509TrustManager;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A server's chain is verified up to a trust anchor (the JDK's cacerts, and a bundle's CA certificates), never trusted
 * by comparing it with the client's certificate.
 */
class TrustManagersTest {

    static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    static X509Certificate certificate(String subject, KeyPair subjectKeys, String issuer, KeyPair issuerKeys, boolean ca) throws Exception {
        Date now = new Date();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(new X500Name(issuer), BigInteger.valueOf(System.nanoTime()),
                new Date(now.getTime() - 60_000), new Date(now.getTime() + 86_400_000L), new X500Name(subject), subjectKeys.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
        return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(issuerKeys.getPrivate())));
    }

    static char[] pem(X509Certificate certificate) throws Exception {
        return ("-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder().encodeToString(certificate.getEncoded())
                + "\n-----END CERTIFICATE-----\n").toCharArray();
    }

    @Test
    void thatAChainEndingInTheClientsCertificateIsNotTrusted() throws Exception {
        KeyPair clientKeys = keyPair();
        X509Certificate client = certificate("CN=business-client", clientKeys, "CN=business-client", clientKeys, false);
        KeyPair attackerKeys = keyPair();
        X509Certificate attacker = certificate("CN=api.example.org", attackerKeys, "CN=api.example.org", attackerKeys, false);

        X509TrustManager trustManager = TrustManagers.create(null);
        assertThrows(CertificateException.class, () -> trustManager.checkServerTrusted(new X509Certificate[]{attacker, client}, "RSA"));
        assertThrows(CertificateException.class, () -> trustManager.checkServerTrusted(new X509Certificate[]{attacker}, "RSA"));
    }

    @Test
    void thatABundlesTrustAnchorsVerifyAServerChain() throws Exception {
        KeyPair caKeys = keyPair();
        X509Certificate ca = certificate("CN=private-ca", caKeys, "CN=private-ca", caKeys, true);
        KeyPair serverKeys = keyPair();
        X509Certificate server = certificate("CN=api.example.org", serverKeys, "CN=private-ca", caKeys, false);
        KeyPair otherKeys = keyPair();
        X509Certificate forged = certificate("CN=api.example.org", otherKeys, "CN=private-ca", otherKeys, false);

        X509TrustManager withoutAnchors = TrustManagers.create(null);
        X509Certificate[] chain = {server, ca};
        assertThrows(CertificateException.class, () -> withoutAnchors.checkServerTrusted(chain, "RSA"));

        X509TrustManager withAnchors = TrustManagers.create(pem(ca));
        assertDoesNotThrow(() -> withAnchors.checkServerTrusted(new X509Certificate[]{server, ca}, "RSA"));
        assertDoesNotThrow(() -> withAnchors.checkServerTrusted(new X509Certificate[]{server}, "RSA"));
        // the issuer's name is not enough: the signature must verify
        assertThrows(CertificateException.class, () -> withAnchors.checkServerTrusted(new X509Certificate[]{forged, ca}, "RSA"));
    }

    @Test
    void thatTrustCertificatesWithoutACertificateAreRefused() {
        char[] notACertificate = "not a certificate".toCharArray();
        assertThrows(RuntimeException.class, () -> TrustManagers.create(notACertificate));
    }
}
