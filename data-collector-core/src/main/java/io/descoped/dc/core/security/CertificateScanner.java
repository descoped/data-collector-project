package io.descoped.dc.core.security;

import io.descoped.secrets.api.SecretManagerClient;
import io.descoped.secrets.api.SecretNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

class CertificateScanner {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateScanner.class);
    private static final String PRIVATE_KEY = "private.key";
    private static final String PUBLIC_CERTIFICATE = "public.certificate";
    private static final String ARCHIVE_CERTIFICATE = "archive.certificate";
    private static final String SECRET_PASSPHRASE = "secret.passphrase";
    private static final String TRUST_CERTIFICATES = "trust.certificates";

    final Path scanDir;
    Map<String, CertificateBundle> bundles;

    CertificateScanner(Path scanDir) {
        this.scanDir = scanDir;
        if (LOG.isTraceEnabled()) {
            LOG.trace("Scan directory: {}", scanDir.toAbsolutePath());
        }
    }

    Map<String, CertificateBundle> getCertificateBundles() {
        Objects.requireNonNull(bundles);
        return bundles;
    }

    void scan() {
        try {
            bundles = findSecurityPropertyFilesAndLoad();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, CertificateBundle> findSecurityPropertyFilesAndLoad() throws IOException {
        Map<String, CertificateBundle> certificateBundles = new LinkedHashMap<>();
        Files.walkFileTree(scanDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file) && file.getFileName().toAbsolutePath().endsWith("secret.properties")) {
                    Path fileNamePath = file.toAbsolutePath();
                    try (SecretManagerClient secretsClient = SecretManagerClient.create(Map.of(
                            "secrets.provider", "safe-configuration",
                            "secrets.property-resource-path", fileNamePath.toString()
                    ))) {
                        return loadBundle(file, secretsClient, certificateBundles);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return certificateBundles;
    }

    private FileVisitResult loadBundle(Path file, SecretManagerClient secretsClient, Map<String, CertificateBundle> certificateBundles) throws IOException {
        CertificateBundle.Builder builder = new CertificateBundle.Builder();

        builder.secretFileNamePath(file.toAbsolutePath());

        String bundleName = file.getParent().getName(file.getParent().getNameCount() - 1).toString();

        if (certificateBundles.containsKey(bundleName)) {
            LOG.warn("The certificate bundle '{}' is already discovered and loaded from: {}", bundleName, certificateBundles.get(bundleName).secretPropertiesPath().orElseThrow());
            return FileVisitResult.TERMINATE;
        }

        boolean isPEMBundle = hasSecret(secretsClient, PRIVATE_KEY) && hasSecret(secretsClient, PUBLIC_CERTIFICATE);
        boolean isP12Bundle = hasSecret(secretsClient, ARCHIVE_CERTIFICATE);

        if (isPEMBundle) {
            if (!readPemBundle(file, secretsClient, bundleName, builder)) {
                return FileVisitResult.TERMINATE;
            }

        } else if (isP12Bundle) {
            if (!readP12Bundle(file, secretsClient, bundleName, builder)) {
                return FileVisitResult.TERMINATE;
            }

        } else {
            LOG.warn("Skipping bundle '{}'. Error with configuration!", bundleName);
        }

        // secrets-client 2 read a missing passphrase as empty; 3 throws
        builder.passphrase(hasSecret(secretsClient, SECRET_PASSPHRASE) ? secretsClient.readChars(SECRET_PASSPHRASE) : new char[0]);

        // optional: the bundle's trust anchors, a PEM file of CA certificates, for servers whose chain is not in cacerts
        if (hasSecret(secretsClient, TRUST_CERTIFICATES)) {
            Path trustCertificatesFile = file.getParent().resolve(secretsClient.readString(TRUST_CERTIFICATES));
            if (!trustCertificatesFile.toFile().exists()) {
                // as for the bundle's other files: an incomplete bundle ends the scan
                LOG.warn("Skipping bundle '{}'. Could not find the file named by {}", bundleName, TRUST_CERTIFICATES);
                return FileVisitResult.TERMINATE;
            }
            builder.trustCertificates(readFileToCharArray(trustCertificatesFile));
        }

        certificateBundles.put(bundleName, builder.build());
        return FileVisitResult.CONTINUE;
    }

    // false when the bundle is incomplete, which ends the scan
    private boolean readPemBundle(Path file, SecretManagerClient secretsClient, String bundleName, CertificateBundle.Builder builder) throws IOException {
        if (validateSecretProperties(secretsClient, file, SECRET_PASSPHRASE, PRIVATE_KEY, PUBLIC_CERTIFICATE)) {
            return false;
        }

        Path privateKeyFile = file.getParent().resolve(secretsClient.readString(PRIVATE_KEY));
        if (!privateKeyFile.toFile().exists()) {
            LOG.warn("Skipping bundle '{}'. Could not find privateKeyFile: {}", bundleName, privateKeyFile.toAbsolutePath());
            return false;
        }
        builder.privateKey(readFileToCharArray(privateKeyFile));

        Path publicCertFile = file.getParent().resolve(secretsClient.readString(PUBLIC_CERTIFICATE));
        if (!publicCertFile.toFile().exists()) {
            LOG.warn("Skipping bundle '{}'. Could not find publicCertFile: {}", bundleName, publicCertFile.toAbsolutePath());
            return false;
        }
        builder.publicCert(readFileToCharArray(publicCertFile));
        return true;
    }

    // false when the bundle is incomplete, which ends the scan
    private boolean readP12Bundle(Path file, SecretManagerClient secretsClient, String bundleName, CertificateBundle.Builder builder) throws IOException {
        if (validateSecretProperties(secretsClient, file, SECRET_PASSPHRASE, ARCHIVE_CERTIFICATE)) {
            return false;
        }

        Path archiveCertFile = file.getParent().resolve(secretsClient.readString(ARCHIVE_CERTIFICATE));
        if (!archiveCertFile.toFile().exists()) {
            LOG.warn("Skipping bundle '{}'. Could not find archiveCertFile: {}", bundleName, archiveCertFile.toAbsolutePath());
            return false;
        }
        builder.archiveCert(Files.readAllBytes(archiveCertFile));
        return true;
    }

    private boolean validateSecretProperties(SecretManagerClient secretsClient, Path file, String... secretProperties) {
        for (String secretProperty : secretProperties) {
            if (!hasSecret(secretsClient, secretProperty)) {
                LOG.warn("Undefined property '{}' in 'secret.passphrase'. Skip loading certificate bundle for: {}", secretProperty, file.toAbsolutePath());
                return true;
            }
        }
        return false;
    }

    private char[] readFileToCharArray(Path file) throws IOException {
        try (FileReader fr = new FileReader(file.toAbsolutePath().toString())) {
            int count;
            int size = (int) file.toFile().length();
            char[] buffer = new char[size];
            do {
                count = fr.read(buffer);
            } while (count != -1);
            return buffer;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CertificateScanner scanner = (CertificateScanner) o;
        return Objects.equals(scanDir, scanner.scanDir) &&
                Objects.equals(bundles, scanner.bundles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scanDir, bundles);
    }

    @Override
    public String toString() {
        return "CertificateScanner{" +
                "scanDir=" + scanDir +
                ", bundles=" + bundles +
                '}';
    }


    private static boolean hasSecret(SecretManagerClient secretsClient, String name) {
        try {
            byte[] value = secretsClient.readBytes(name);
            Arrays.fill(value, (byte) 0);
            return true;
        } catch (SecretNotFoundException e) {
            return false;
        }
    }
}
