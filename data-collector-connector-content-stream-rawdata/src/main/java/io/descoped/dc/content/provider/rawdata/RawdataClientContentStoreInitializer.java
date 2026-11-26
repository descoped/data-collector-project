package io.descoped.dc.content.provider.rawdata;

import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.rawdata.api.RawdataClient;
import io.descoped.secrets.api.SecretManagerClient;
import io.descoped.secrets.api.SecretNotFoundException;
import io.descoped.service.provider.api.ProviderName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Creates the {@code rawdata} content store from the configuration: the rawdata client provider and, optionally, the
 * encryption key and salt or the secret provider that holds them.
 */
@ProviderName("rawdata")
public class RawdataClientContentStoreInitializer implements ContentStoreInitializer {

    /**
     * Creates the initializer; the service loader calls it.
     */
    public RawdataClientContentStoreInitializer() {
        // no state
    }

    private static final Logger LOG = LoggerFactory.getLogger(RawdataClientContentStoreInitializer.class);

    private static final String ENCRYPTION_KEY = "rawdata.encryption.key";
    private static final String ENCRYPTION_SALT = "rawdata.encryption.salt";
    private static final String SECRETS_PROVIDER = "secrets.provider";
    private static final String DYNAMIC_CONFIGURATION = "dynamic-configuration";
    private static final String DYNAMIC = "dynamic-secret-configuration";
    private static final String GOOGLE = "google-secret-manager";

    @Override
    public Set<String> configurationKeys() {
        return Set.of("rawdata.client.provider");
    }

    /*
     * Encryption of content, optional (the rules of 2.x):
     *
     * rawdata.encryption.provider = dynamic-secret-configuration | google-secret-manager (dynamic-configuration is
     *                                accepted for dynamic-secret-configuration, the provider's name in secrets-client 3)
     * rawdata.encryption.gcp.projectId => google-secret-manager
     * rawdata.encryption.gcp.serviceAccountKeyPath => google-secret-manager: a service-account key;
     *                                dynamic-secret-configuration: a properties file holding the key and salt
     * rawdata.encryption.key  = the password, or with google-secret-manager the name of its secret
     * rawdata.encryption.salt = the salt, or with google-secret-manager the name of its secret
     *
     * Without a provider, the key and salt are read from this configuration. Without any rawdata.encryption.* key the
     * content is not encrypted. Once any is set, encryption fails closed: an unknown provider, or a key or salt that is
     * missing or empty, stops the store from being created; content is never written in clear instead.
     */

    @Override
    public ContentStore initialize(Map<String, String> configuration) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Content stream connector: {}", configuration.get("content.stream.connector"));

            LOG.info("Rawdata client provider: {}", configuration.get("rawdata.client.provider"));
            LOG.info("Rawdata encryption enabled: {}", isEncryptionConfigured(configuration));
        }

        if (!isEncryptionConfigured(configuration)) {
            return new RawdataClientContentStore(RawdataClient.create(configuration), null, null);
        }

        String encryptionProvider = configuration.get("rawdata.encryption.provider");
        if (encryptionProvider != null && !Set.of(DYNAMIC, DYNAMIC_CONFIGURATION, GOOGLE).contains(encryptionProvider)) {
            throw new IllegalStateException("Unknown rawdata.encryption.provider");
        }
        Map<String, String> secretsConfiguration = secretsConfiguration(encryptionProvider, configuration);
        if (secretsConfiguration.isEmpty()) {
            throw new IllegalStateException("Rawdata encryption is configured, but " + ENCRYPTION_KEY + " and " + ENCRYPTION_SALT
                    + " are not both set");
        }

        LOG.debug("Load encryption credentials from: {}", encryptionProvider == null ? "application configuration" : encryptionProvider);
        boolean google = GOOGLE.equals(encryptionProvider);
        String keySecretName = google ? Optional.ofNullable(configuration.get(ENCRYPTION_KEY)).orElseThrow() : ENCRYPTION_KEY;
        String saltSecretName = google ? Optional.ofNullable(configuration.get(ENCRYPTION_SALT)).orElseThrow() : ENCRYPTION_SALT;
        char[] encryptionKey = null;
        byte[] encryptionSalt = null;
        try (SecretManagerClient secretManagerClient = SecretManagerClient.create(secretsConfiguration)) {
            // secrets-client 2 returned null for a missing secret; 3 throws
            encryptionKey = readOrNull(() -> secretManagerClient.readChars(keySecretName));
            encryptionSalt = readOrNull(() -> secretManagerClient.readBytes(saltSecretName));
            if (encryptionKey == null || encryptionKey.length == 0) {
                throw new IllegalStateException("Rawdata encryption is configured, but its key is missing or empty");
            }
            if (encryptionSalt == null || encryptionSalt.length == 0) {
                throw new IllegalStateException("Rawdata encryption is configured, but its salt is missing or empty");
            }
            return new RawdataClientContentStore(RawdataClient.create(configuration), encryptionKey, encryptionSalt);
        } finally {
            if (encryptionKey != null) Arrays.fill(encryptionKey, '\u0000');
            if (encryptionSalt != null) Arrays.fill(encryptionSalt, (byte) 0);
        }
    }

    // any rawdata.encryption.* key turns encryption on, and then it must succeed
    static boolean isEncryptionConfigured(Map<String, String> configuration) {
        return configuration.keySet().stream().anyMatch(key -> key.startsWith("rawdata.encryption."));
    }

    static Map<String, String> secretsConfiguration(String encryptionProvider, Map<String, String> configuration) {
        Map<String, String> secrets = new LinkedHashMap<>();
        String keyPath = configuration.get("rawdata.encryption.gcp.serviceAccountKeyPath");
        if (DYNAMIC.equals(encryptionProvider) || DYNAMIC_CONFIGURATION.equals(encryptionProvider)) {
            // secrets-client 3 names this provider dynamic-configuration
            secrets.put(SECRETS_PROVIDER, DYNAMIC_CONFIGURATION);
            // the log lines are as in 2.x
            if (keyPath == null) {
                LOG.info("Use custom encryption credentials configuration: {}", keyPath);
                putInline(secrets, configuration);
            } else {
                LOG.info("Use application configuration for encryption credentials");
                secrets.put("secrets.property-resource-path", keyPath);
            }
        } else if (GOOGLE.equals(encryptionProvider)) {
            LOG.info("Use Google Secret Manager for encryption credentials");
            secrets.put(SECRETS_PROVIDER, encryptionProvider);
            secrets.put("secrets.project-id", Optional.ofNullable(configuration.get("rawdata.encryption.gcp.projectId")).orElseThrow());
            // Application Default Credentials (2.x: Compute Engine) when no service account key is set
            if (keyPath != null) {
                secrets.put("secrets.service-account-key-path", keyPath);
            }
        } else if (configuration.containsKey(ENCRYPTION_KEY) && configuration.containsKey(ENCRYPTION_SALT)) {
            LOG.info("Fallback application configuration for encryption credentials");
            secrets.put(SECRETS_PROVIDER, DYNAMIC_CONFIGURATION);
            putInline(secrets, configuration);
        }
        return secrets;
    }

    // The provider reads the secrets it is given; with 2.x it read them from the whole application configuration.
    // Only these two are ever read.
    private static void putInline(Map<String, String> secrets, Map<String, String> configuration) {
        for (String key : new String[]{ENCRYPTION_KEY, ENCRYPTION_SALT}) {
            String value = configuration.get(key);
            if (value != null) {
                secrets.put("secrets.values." + key, value);
            }
        }
    }

    private static <T> T readOrNull(java.util.function.Supplier<T> read) {
        try {
            return read.get();
        } catch (SecretNotFoundException e) {
            return null;
        }
    }
}
