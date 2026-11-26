package io.descoped.dc.api.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceCache {

    private final Map<Class<?>, Object> cache = new ConcurrentHashMap<>();

    /**
     * Creates an empty instance.
     */
    public InstanceCache() {
        // the state is in the fields
    }

    public <R> R add(Class<?> clazz, R instance) {
        cache.put(clazz, instance);
        return instance;
    }

    public boolean has(Class<?> clazz) {
        return cache.containsKey(clazz);
    }

    @SuppressWarnings("unchecked") // the caller names the cached instance's type
    public <R> R get(Class<?> clazz) {
        return (R) cache.get(clazz);
    }

    private static class InstanceCacheHolder {
        private static final InstanceCache INSTANCE = new InstanceCache();
    }

    public static InstanceCache instance() {
        return InstanceCacheHolder.INSTANCE;
    }
}
