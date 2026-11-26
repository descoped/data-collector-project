package io.descoped.dc.core.security;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.security.BusinessSSLResource;
import io.descoped.dc.core.executor.Worker;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static io.descoped.dc.api.Builders.context;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.security;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * The bundles are made in a temporary directory from a key pair and a self-signed certificate generated for the test.
 */
class CertificateScannerTest {

    static final String PASSPHRASE = "changeit";

    static KeyPair keyPair;
    static String privateKeyPem;
    static String publicCertPem;
    static byte[] archive;

    @BeforeAll
    static void generateKeyMaterial() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        X500Name subject = new X500Name("CN=dc-test");
        Instant now = Instant.now();
        X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(subject, BigInteger.ONE,
                Date.from(now.minus(Duration.ofDays(1))), Date.from(now.plus(Duration.ofDays(1))), subject, keyPair.getPublic());
        X509Certificate certificate = new JcaX509CertificateConverter()
                .getCertificate(certificateBuilder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));

        privateKeyPem = pem(keyPair.getPrivate());
        publicCertPem = pem(certificate);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("dc-test", keyPair.getPrivate(), PASSPHRASE.toCharArray(), new Certificate[]{certificate});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        keyStore.store(out, PASSPHRASE.toCharArray());
        archive = out.toByteArray();
    }

    static String pem(Object object) throws IOException {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(object);
        }
        return writer.toString();
    }

    static Path bundle(Path scanDir, String bundleName, Map<String, String> secrets, Map<String, byte[]> files) throws IOException {
        Path bundleDir = Files.createDirectories(scanDir.resolve(bundleName));
        StringBuilder properties = new StringBuilder();
        secrets.forEach((key, value) -> properties.append(key).append('=').append(value).append('\n'));
        Files.writeString(bundleDir.resolve("secret.properties"), properties.toString());
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            Files.write(bundleDir.resolve(file.getKey()), file.getValue());
        }
        return bundleDir;
    }

    static Path pemBundle(Path scanDir, String bundleName) throws IOException {
        return bundle(scanDir, bundleName,
                Map.of("secret.passphrase", PASSPHRASE, "private.key", "key.pem", "public.certificate", "cert.pem"),
                Map.of("key.pem", privateKeyPem.getBytes(StandardCharsets.UTF_8), "cert.pem", publicCertPem.getBytes(StandardCharsets.UTF_8)));
    }

    static Path p12Bundle(Path scanDir, String bundleName) throws IOException {
        return bundle(scanDir, bundleName,
                Map.of("secret.passphrase", PASSPHRASE, "archive.certificate", "cert.p12"),
                Map.of("cert.p12", archive));
    }

    static Map<String, CertificateBundle> scan(Path scanDir) {
        CertificateScanner scanner = new CertificateScanner(scanDir);
        scanner.scan();
        return scanner.getCertificateBundles();
    }

    @Test
    void thatPemBundleIsLoaded(@TempDir Path scanDir) throws IOException {
        Path bundleDir = pemBundle(scanDir, "pem-bundle");

        Map<String, CertificateBundle> bundles = scan(scanDir);

        CertificateBundle bundle = bundles.get("pem-bundle");
        assertNotNull(bundle);
        assertFalse(bundle.isArchive());
        assertEquals(bundleDir.resolve("secret.properties").toAbsolutePath(), bundle.secretPropertiesPath().orElseThrow());
        assertArrayEquals(PASSPHRASE.toCharArray(), bundle.passphrase);
        assertEquals(privateKeyPem, new String(bundle.privateKey));
        assertEquals(publicCertPem, new String(bundle.publicCert));
    }

    @Test
    void thatP12BundleIsLoaded(@TempDir Path scanDir) throws IOException {
        p12Bundle(scanDir, "p12-bundle");

        CertificateBundle bundle = scan(scanDir).get("p12-bundle");

        assertNotNull(bundle);
        assertTrue(bundle.isArchive());
        assertArrayEquals(archive, bundle.archiveCert);
    }

    @Test
    void thatFactoryBuildsSslContextsFromScannedBundles(@TempDir Path scanDir) throws IOException {
        pemBundle(scanDir, "pem-bundle");
        p12Bundle(scanDir, "p12-bundle");

        CertificateFactory factory = CertificateFactory.scanAndCreate(scanDir);

        assertEquals(2, factory.getBundleNames().size());
        for (String bundleName : factory.getBundleNames()) {
            CertificateContext context = factory.getCertificateContext(bundleName);
            assertNotNull(context.sslContext(), bundleName);
            assertNotNull(context.trustManager(), bundleName);
        }
        assertThrows(IllegalArgumentException.class, () -> factory.getCertificateContext("no-such-bundle"));
    }

    // the four encodings SslPEMKeyStore reads a private key in
    static String privateKeyPem(String encoding) throws Exception {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            switch (encoding) {
                case "openssl" -> pemWriter.writeObject(keyPair.getPrivate());
                case "openssl-encrypted" -> pemWriter.writeObject(keyPair.getPrivate(),
                        new JcePEMEncryptorBuilder("AES-256-CBC").build(PASSPHRASE.toCharArray()));
                case "pkcs8" -> pemWriter.writeObject(new JcaPKCS8Generator(keyPair.getPrivate(), null));
                case "pkcs8-encrypted" -> pemWriter.writeObject(new JcaPKCS8Generator(keyPair.getPrivate(),
                        new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC).setPassword(PASSPHRASE.toCharArray()).build()));
                default -> throw new IllegalArgumentException(encoding);
            }
        }
        return writer.toString();
    }

    @ParameterizedTest
    @ValueSource(strings = {"openssl", "openssl-encrypted", "pkcs8", "pkcs8-encrypted"})
    void thatPemKeyIsReadInEachEncoding(String encoding, @TempDir Path scanDir) throws Exception {
        bundle(scanDir, "pem-bundle",
                Map.of("secret.passphrase", PASSPHRASE, "private.key", "key.pem", "public.certificate", "cert.pem"),
                Map.of("key.pem", privateKeyPem(encoding).getBytes(StandardCharsets.UTF_8), "cert.pem", publicCertPem.getBytes(StandardCharsets.UTF_8)));

        CertificateContext context = CertificateFactory.scanAndCreate(scanDir).getCertificateContext("pem-bundle");

        assertEquals(keyPair.getPublic(), context.keyPair().getPublic());
        assertEquals(keyPair.getPrivate(), context.keyPair().getPrivate());
    }

    // an archive without a private key gives no key pair
    @Test
    void thatArchiveWithoutPrivateKeyIsRefused(@TempDir Path scanDir) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setEntry("secret", new KeyStore.SecretKeyEntry(new SecretKeySpec(new byte[16], "AES")),
                new KeyStore.PasswordProtection(PASSPHRASE.toCharArray()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        keyStore.store(out, PASSPHRASE.toCharArray());
        bundle(scanDir, "p12-bundle", Map.of("secret.passphrase", PASSPHRASE, "archive.certificate", "cert.p12"),
                Map.of("cert.p12", out.toByteArray()));

        assertThrows(RuntimeException.class, () -> CertificateFactory.scanAndCreate(scanDir));
    }

    @Test
    void thatMissingPassphraseSkipsTheBundle(@TempDir Path scanDir) throws IOException {
        bundle(scanDir, "pem-bundle",
                Map.of("private.key", "key.pem", "public.certificate", "cert.pem"),
                Map.of("key.pem", privateKeyPem.getBytes(StandardCharsets.UTF_8), "cert.pem", publicCertPem.getBytes(StandardCharsets.UTF_8)));
        bundle(scanDir, "p12-bundle", Map.of("archive.certificate", "cert.p12"), Map.of("cert.p12", archive));

        assertTrue(scan(scanDir).isEmpty());
    }

    @Test
    void thatMissingPrivateKeyFileSkipsTheBundle(@TempDir Path scanDir) throws IOException {
        bundle(scanDir, "pem-bundle",
                Map.of("secret.passphrase", PASSPHRASE, "private.key", "key.pem", "public.certificate", "cert.pem"),
                Map.of("cert.pem", publicCertPem.getBytes(StandardCharsets.UTF_8)));

        assertTrue(scan(scanDir).isEmpty());
    }

    @Test
    void thatMissingPublicCertificateFileSkipsTheBundle(@TempDir Path scanDir) throws IOException {
        bundle(scanDir, "pem-bundle",
                Map.of("secret.passphrase", PASSPHRASE, "private.key", "key.pem", "public.certificate", "cert.pem"),
                Map.of("key.pem", privateKeyPem.getBytes(StandardCharsets.UTF_8)));

        assertTrue(scan(scanDir).isEmpty());
    }

    @Test
    void thatMissingArchiveFileSkipsTheBundle(@TempDir Path scanDir) throws IOException {
        bundle(scanDir, "p12-bundle", Map.of("secret.passphrase", PASSPHRASE, "archive.certificate", "cert.p12"), Map.of());

        assertTrue(scan(scanDir).isEmpty());
    }

    @Test
    void thatBundleWithoutCertificatesIsKeptWithItsPassphrase(@TempDir Path scanDir) throws IOException {
        bundle(scanDir, "empty-bundle", Map.of("secret.passphrase", PASSPHRASE), Map.of());

        CertificateBundle bundle = scan(scanDir).get("empty-bundle");

        assertNotNull(bundle);
        assertArrayEquals(PASSPHRASE.toCharArray(), bundle.passphrase);
    }

    @Test
    void thatDuplicateBundleNameEndsTheScan(@TempDir Path scanDir) throws IOException {
        pemBundle(scanDir.resolve("a"), "same-name");
        pemBundle(scanDir.resolve("b"), "same-name");

        Map<String, CertificateBundle> bundles = scan(scanDir);

        assertEquals(1, bundles.size());
        assertTrue(bundles.containsKey("same-name"));
    }

    static Worker.WorkerBuilder workerWithBundle(String bundleName) {
        return Worker.newBuilder()
                .specification(Specification.start("test", "secured", "page")
                        .configure(context().topic("topic"))
                        .configure(security().sslBundleName(bundleName))
                        .function(get("page").url("https://localhost/")));
    }

    @Test
    void thatWorkerUsesTheBundleScannedFromItsDirectory(@TempDir Path scanDir) throws IOException {
        pemBundle(scanDir, "pem-bundle");

        Worker worker = workerWithBundle("pem-bundle").buildCertificateFactory(scanDir).build();

        CertificateFactory factory = worker.context().services().get(CertificateFactory.class);
        assertEquals(Set.of("pem-bundle"), factory.getBundleNames());
        assertNotNull(worker.context().services().get(Client.class));
    }

    @Test
    void thatWorkerUsesTheBundleItsSupplierGives() {
        BusinessSSLResource resource = new BusinessSSLResource() {
            @Override
            public String bundleName() {
                return "p12-bundle";
            }

            @Override
            public String getType() {
                return "p12";
            }

            @Override
            public char[] publicCertificate() {
                return new char[0];
            }

            @Override
            public char[] privateCertificate() {
                return new char[0];
            }

            @Override
            public byte[] archiveCertificate() {
                return archive.clone();
            }

            @Override
            public char[] passphrase() {
                return PASSPHRASE.toCharArray();
            }

            @Override
            public void close() {
                // nothing to release: the key material is in memory
            }
        };

        Worker worker = workerWithBundle("p12-bundle").useBusinessSSLResourceSupplier(() -> resource).build();

        CertificateFactory factory = worker.context().services().get(CertificateFactory.class);
        assertEquals(Set.of("p12-bundle"), factory.getBundleNames());
    }
}
