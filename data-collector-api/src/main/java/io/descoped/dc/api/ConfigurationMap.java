package io.descoped.dc.api;

import java.util.Map;

/**
 * Configuration keys and values, as strings.
 */
public class ConfigurationMap {

    private final Map<String, String> configMap;

    /**
     * Creates the configuration on a map.
     *
     * @param configMap the keys and values; kept, not copied
     */
    public ConfigurationMap(Map<String, String> configMap) {
        this.configMap = configMap;
    }

    /**
     * Whether a key is set.
     *
     * @param key the key
     * @return {@code true} if the key is set
     */
    public boolean contains(String key) {
        return configMap.containsKey(key);
    }

    /**
     * The value of a key.
     *
     * @param key the key
     * @return the value, or {@code null} if the key is not set
     */
    public String get(String key) {
        return configMap.get(key);
    }

    /**
     * The keys and values.
     *
     * @return the backing map, not a copy
     */
    public Map<String, String> asMap() {
        return configMap;
    }

    /**
     * Sets a key.
     *
     * @param key   the key
     * @param value the value
     */
    public void put(String key, String value) {
        configMap.put(key, value);
    }
}
