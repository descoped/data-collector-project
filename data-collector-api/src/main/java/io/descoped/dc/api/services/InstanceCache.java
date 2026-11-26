package io.descoped.dc.api.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe cache of instances by class.
 */
public class InstanceCache {

    private final Map<Class<?>, Object> cache = new ConcurrentHashMap<>();

    /**
     * Creates an empty instance.
     */
    public InstanceCache() {
        // the state is in the fields
    }

    /**
     * Caches an instance under a class, replacing any cached.
     *
     * @param clazz    the class
     * @param instance the instance
     * @param <R>      the instance's type
     * @return the instance
     */
    public <R> R add(Class<?> clazz, R instance) {
        cache.put(clazz, instance);
        return instance;
    }

    /**
     * Whether an instance is cached under a class.
     *
     * @param clazz the class
     * @return {@code true} if one is cached
     */
    public boolean has(Class<?> clazz) {
        return cache.containsKey(clazz);
    }

    /**
     * The instance cached under a class.
     *
     * @param clazz the class
     * @param <R>   the instance's type
     * @return the instance, or {@code null} if none is cached
     */
    @SuppressWarnings("unchecked") // the caller names the cached instance's type
    public <R> R get(Class<?> clazz) {
        return (R) cache.get(clazz);
    }

    private static class InstanceCacheHolder {
        private static final InstanceCache INSTANCE = new InstanceCache();
    }

    /**
     * The shared cache.
     *
     * @return the cache
     */
    public static InstanceCache instance() {
        return InstanceCacheHolder.INSTANCE;
    }
}
