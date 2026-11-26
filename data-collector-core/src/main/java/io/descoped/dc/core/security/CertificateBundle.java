package io.descoped.dc.core.security;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

class CertificateBundle {
    private final Path secretPropertiesPath;
    final char[] passphrase;
    final char[] privateKey;  // base64 pem certificate
    final char[] publicCert;  // base64 pem certificate
    final byte[] archiveCert; // binary certificate archive
    final char[] trustCertificates; // the bundle's trust anchors, PEM CA certificates, or null
    final String protocol = "TLSv1.2";

    private CertificateBundle(Path secretPropertiesPath, char[] passphrase, char[] privateKey, char[] publicCert, char[] trustCertificates) {
        this.secretPropertiesPath = secretPropertiesPath;
        this.passphrase = passphrase;
        this.privateKey = privateKey;
        this.publicCert = publicCert;
        this.archiveCert = null;
        this.trustCertificates = trustCertificates;
    }

    private CertificateBundle(Path secretPropertiesPath, char[] passphrase, byte[] archiveCert, char[] trustCertificates) {
        this.secretPropertiesPath = secretPropertiesPath;
        this.passphrase = passphrase;
        this.privateKey = null;
        this.publicCert = null;
        this.archiveCert = archiveCert;
        this.trustCertificates = trustCertificates;
    }

    public Optional<Path> secretPropertiesPath() {
        return Optional.ofNullable(secretPropertiesPath);
    }

    boolean isArchive() {
        return privateKey == null && publicCert == null && archiveCert != null;
    }

    void dispose() {
        emptyCharArray(passphrase);
        emptyCharArray(privateKey);
        emptyCharArray(publicCert);
        emptyByteArray(archiveCert);
        emptyCharArray(trustCertificates);
    }

    void emptyByteArray(byte[] array) {
        if (array == null) {
            return;
        }
        Arrays.fill(array, (byte) 0);
    }

    void emptyCharArray(char[] array) {
        if (array == null) {
            return;
        }
        Arrays.fill(array, '\u0000');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CertificateBundle that = (CertificateBundle) o;
        return Arrays.equals(passphrase, that.passphrase) &&
                Arrays.equals(privateKey, that.privateKey) &&
                Arrays.equals(publicCert, that.publicCert) &&
                Arrays.equals(archiveCert, that.archiveCert) &&
                Arrays.equals(trustCertificates, that.trustCertificates) &&
                protocol.equals(that.protocol);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(protocol);
        result = 31 * result + Arrays.hashCode(passphrase);
        result = 31 * result + Arrays.hashCode(privateKey);
        result = 31 * result + Arrays.hashCode(publicCert);
        result = 31 * result + Arrays.hashCode(archiveCert);
        result = 31 * result + Arrays.hashCode(trustCertificates);
        return result;
    }

    @Override
    public String toString() {
        return "CertificateBundle{" +
                "secretPropertiesPath=" + secretPropertiesPath +
                '}';
    }

    static class Builder {
        private Path secretFileNamePath;
        private char[] passphrase;
        private char[] privateKey;
        private char[] publicCert;
        private byte[] archiveCert;
        private char[] trustCertificates;

        Builder secretFileNamePath(Path secretFileNamePath) {
            this.secretFileNamePath = secretFileNamePath;
            return this;
        }

        Builder passphrase(char[] passphrase) {
            this.passphrase = passphrase;
            return this;
        }

        Builder privateKey(char[] privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        Builder publicCert(char[] publicCert) {
            this.publicCert = publicCert;
            return this;
        }

        Builder archiveCert(byte[] archiveCert) {
            this.archiveCert = archiveCert;
            return this;
        }

        Builder trustCertificates(char[] trustCertificates) {
            this.trustCertificates = trustCertificates;
            return this;
        }

        CertificateBundle build() {
            if (privateKey != null && publicCert != null) {
                return new CertificateBundle(secretFileNamePath, passphrase, privateKey, publicCert, trustCertificates);
            } else {
                return new CertificateBundle(secretFileNamePath, passphrase, archiveCert, trustCertificates);
            }
        }
    }
}
