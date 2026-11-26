package io.descoped.dc.content.provider.rawdata;

import io.descoped.dc.api.content.ContentStore;
import io.descoped.rawdata.api.RawdataClient;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Once any rawdata.encryption.* key is set, the store is created with encryption or not at all: content is never
 * written in clear instead.
 */
class EncryptionFailsClosedTest {

    static Map<String, String> configuration(String... keyValues) {
        Map<String, String> configuration = new LinkedHashMap<>();
        configuration.put("content.stream.connector", "rawdata");
        configuration.put("rawdata.client.provider", "memory");
        for (int i = 0; i < keyValues.length; i += 2) {
            configuration.put(keyValues[i], keyValues[i + 1]);
        }
        return configuration;
    }

    static void assertRefused(String message, Map<String, String> configuration) {
        RawdataClientContentStoreInitializer initializer = new RawdataClientContentStoreInitializer();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> initializer.initialize(configuration));
        assertEquals(message, e.getMessage());
    }

    @Test
    void thatNoEncryptionSettingMeansNoEncryption() throws Exception {
        try (ContentStore store = new RawdataClientContentStoreInitializer().initialize(configuration())) {
            assertNotNull(store.contentStream());
        }
    }

    @Test
    void thatKeyAndSaltEncrypt() throws Exception {
        try (ContentStore store = new RawdataClientContentStoreInitializer().initialize(configuration(
                "rawdata.encryption.key", "password", "rawdata.encryption.salt", "salt"))) {
            assertNotNull(store.contentStream());
        }
    }

    @Test
    void thatAKeyWithoutASaltIsRefused() {
        assertRefused("Rawdata encryption is configured, but rawdata.encryption.key and rawdata.encryption.salt are not both set",
                configuration("rawdata.encryption.key", "password"));
        assertRefused("Rawdata encryption is configured, but rawdata.encryption.key and rawdata.encryption.salt are not both set",
                configuration("rawdata.encryption.salt", "salt"));
    }

    @Test
    void thatAnEmptyKeyOrSaltIsRefused() {
        assertRefused("Rawdata encryption is configured, but its key is missing or empty",
                configuration("rawdata.encryption.key", "", "rawdata.encryption.salt", "salt"));
        assertRefused("Rawdata encryption is configured, but its salt is missing or empty",
                configuration("rawdata.encryption.key", "password", "rawdata.encryption.salt", ""));
    }

    @Test
    void thatAProviderWithAMissingSecretIsRefused() {
        assertRefused("Rawdata encryption is configured, but its salt is missing or empty",
                configuration("rawdata.encryption.provider", "dynamic-secret-configuration", "rawdata.encryption.key", "password"));
    }

    @Test
    void thatAnUnknownProviderIsRefused() {
        assertRefused("Unknown rawdata.encryption.provider",
                configuration("rawdata.encryption.provider", "google-secrets-manager",
                        "rawdata.encryption.key", "key-secret", "rawdata.encryption.salt", "salt-secret"));
    }

    @Test
    void thatTheStoreRefusesHalfAKey() {
        RawdataClient client = RawdataClient.create(configuration());
        char[] key = "password".toCharArray();
        byte[] salt = "salt".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> new RawdataClientContentStore(client, key, null));
        assertThrows(IllegalArgumentException.class, () -> new RawdataClientContentStore(client, null, salt));
    }
}
