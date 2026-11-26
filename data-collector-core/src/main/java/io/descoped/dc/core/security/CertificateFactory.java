package io.descoped.dc.core.security;

import io.descoped.dc.api.security.BusinessSSLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The certificate contexts of the loaded certificate bundles, by bundle name. A bundle's secrets are cleared from
 * memory once its context is built.
 */
public class CertificateFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateFactory.class);

    private final Map<String, CertificateContext> certificateContextMap;

    private CertificateFactory(Map<String, CertificateContext> certificateContextMap) {
        this.certificateContextMap = certificateContextMap;
    }

    /**
     * The names of the loaded bundles.
     *
     * @return the bundle names
     */
    public Set<String> getBundleNames() {
        return certificateContextMap.keySet();
    }

    /**
     * The certificate context of a bundle.
     *
     * @param bundleName the bundle's name
     * @return the bundle's context
     * @throws IllegalArgumentException when no bundle has the name
     */
    public CertificateContext getCertificateContext(String bundleName) {
        if (!certificateContextMap.containsKey(bundleName)) {
            throw new IllegalArgumentException("Unable to resolve certificate bundle: " + bundleName);
        }
        return certificateContextMap.get(bundleName);
    }

    static class Builder {
        Map<String, CertificateBundle> bundles = new LinkedHashMap<>();

        Builder bundle(String bundleName, CertificateBundle bundle) {
            bundles.put(bundleName, bundle);
            return this;
        }

        CertificateFactory build() {
            Map<String, CertificateContext> certificateContextMap = new LinkedHashMap<>();
            for (Map.Entry<String, CertificateBundle> entry : bundles.entrySet()) {
                CertificateBundle certificateBundle = entry.getValue();
                try {
                    SslKeyStore sslKeyStore = certificateBundle.isArchive() ? new SslP12KeyStore(certificateBundle) : new SslPEMKeyStore(certificateBundle);
                    String bundleName = entry.getKey();
                    CertificateContext businessSSLContext = sslKeyStore.buildSSLContext();
                    LOG.info("Loaded certificate {} bundle: {}", (certificateBundle.isArchive() ? "P12" : "PEM"), bundleName);
                    certificateContextMap.put(bundleName, businessSSLContext);
                } finally {
                    certificateBundle.dispose(); // dispose secrets from heap
                }
            }
            return new CertificateFactory(certificateContextMap);
        }
    }

    /**
     * Scans the directory tree for {@code secret.properties} files and loads each as a bundle, PEM or PKCS#12, named
     * after the file's directory. An incomplete bundle or a bundle name found twice ends the scan.
     *
     * @param scanDirectory the directory to scan
     * @return the factory with the loaded bundles
     * @throws RuntimeException when the directory cannot be read
     */
    public static CertificateFactory scanAndCreate(Path scanDirectory) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Certificate location: {}", scanDirectory.toAbsolutePath().normalize());
        }
        CertificateScanner scanner = new CertificateScanner(scanDirectory); // TODO refactor CertificateScanner.findSecurityPropertyFilesAndLoad to use BusinessSSLResource
        scanner.scan();
        CertificateFactory.Builder factoryBuilder = new CertificateFactory.Builder();
        scanner.getCertificateBundles().forEach(factoryBuilder::bundle);
        return factoryBuilder.build();
    }

    /**
     * Loads the bundle of the resource, PEM or PKCS#12, and closes the resource.
     *
     * @param businessSSLResource the bundle's resource
     * @return the factory with the one bundle
     * @throws NullPointerException when the resource is {@code null}
     */
    public static CertificateFactory create(BusinessSSLResource businessSSLResource) {
        Objects.requireNonNull(businessSSLResource);
        // load secrets
        try (businessSSLResource) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Create Certificate: {}", businessSSLResource.bundleName());
            }
            CertificateFactory.Builder factoryBuilder = new CertificateFactory.Builder();

            CertificateBundle.Builder bundleBuilder = new CertificateBundle.Builder();
            if (businessSSLResource.isPEM()) {
                bundleBuilder.publicCert(businessSSLResource.publicCertificate());
                bundleBuilder.privateKey(businessSSLResource.privateCertificate());
            } else {
                bundleBuilder.archiveCert(businessSSLResource.archiveCertificate());
            }
            bundleBuilder.passphrase(businessSSLResource.passphrase());
            bundleBuilder.trustCertificates(businessSSLResource.trustCertificates());

            factoryBuilder.bundle(businessSSLResource.bundleName(), bundleBuilder.build());
            return factoryBuilder.build();
        }
    }

}
