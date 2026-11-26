package io.descoped.dc.api.services;

import java.util.Map;

/**
 * The services of an execution, one instance per class.
 */
public interface Services {
    /**
     * Whether a service is registered for a class.
     *
     * @param clazz the class
     * @param <R>   the class's type
     * @return {@code true} if one is registered
     */
    <R> boolean contains(Class<R> clazz);

    /**
     * The service registered for a class.
     *
     * @param clazz the class
     * @param <R>   the service's type
     * @return the service, or {@code null} if none is registered
     */
    <R> R get(Class<R> clazz);

    /**
     * The services by class.
     *
     * @return a copy of the services
     */
    Map<Class<?>, Object> asMap();

    /**
     * Registers a service for a class, replacing any registered.
     *
     * @param clazz    the class
     * @param instance the service
     * @param <R>      the class's type
     * @return these services
     */
    <R> Services register(Class<R> clazz, Object instance);

    /**
     * Creates empty services.
     *
     * @return the services
     */
    static Services create() {
        return new InjectionParameters();
    }
}
