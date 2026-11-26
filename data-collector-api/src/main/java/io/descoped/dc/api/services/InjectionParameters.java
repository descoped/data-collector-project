package io.descoped.dc.api.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The thread-safe implementation of {@link Services}.
 */
public class InjectionParameters implements Services {

    private final Map<Class<?>, AtomicReference<Object>> instanceMap = new ConcurrentHashMap<>();

    /**
     * Creates empty services.
     */
    public InjectionParameters() {
        // no parameters until they are put
    }

    @Override
    public <R> boolean contains(Class<R> clazz) {
        return instanceMap.containsKey(clazz);
    }

    @SuppressWarnings("unchecked") // the parameter registered for a class is of that class
    @Override
    public <R> R get(Class<R> clazz) {
        try {
            return (R) instanceMap.get(clazz).get();
        } catch (NullPointerException npe) {
            return null;
        }
    }

    @Override
    public Map<Class<?>, Object> asMap() {
        return instanceMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().get()));
    }

    @Override
    public <R> Services register(Class<R> clazz, Object instance) {
        instanceMap.put(clazz, new AtomicReference<>(instance));
        return this;
    }

    /**
     * Registers the services of another instance, replacing those of the same class.
     *
     * @param injectionParameters the other instance
     */
    public void putAll(InjectionParameters injectionParameters) {
        for (Map.Entry<Class<?>, AtomicReference<Object>> entry : injectionParameters.instanceMap.entrySet()) {
            instanceMap.put(entry.getKey(), entry.getValue());
        }
    }

}
