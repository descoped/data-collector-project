package io.descoped.dc.application.health;

import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;
import io.descoped.dc.api.health.HealthResourceExclude;
import io.descoped.dc.application.server.ApplicationException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Creates and holds the health resources: the discovered ones and those added at run time by id.
 */
public class HealthResourceFactory {

    private final List<Class<? extends HealthResource>> healthResourceClasses;
    private final Map<Class<? extends HealthResource>, ? extends HealthResource> healthResources;
    private final Map<UUID, HealthResource> dynamicHealthResources;

    private HealthResourceFactory() {
        healthResourceClasses = loadHealthResources();
        healthResources = createHealthResources(healthResourceClasses);
        dynamicHealthResources = new LinkedHashMap<>();
    }

    /**
     * Creates the factory with the discovered resources.
     *
     * @return the factory
     */
    public static HealthResourceFactory create() {
        return new HealthResourceFactory();
    }

    /**
     * The discovered resource of a class.
     *
     * @param healthResourceClass the class
     * @param <R>                 the resource's type
     * @return the resource, or {@code null}
     */
    public <R extends HealthResource> R getHealthResource(Class<R> healthResourceClass) {
        return healthResourceClass.cast(healthResources.get(healthResourceClass));
    }

    /**
     * Every resource, discovered and added.
     *
     * @return the resources
     */
    public List<HealthResource> getHealthResources() {
        SortedSet<HealthResource> resourceList = new TreeSet<>(new HealthResourcePriorityComparator());
        resourceList.addAll(healthResources.values());
        resourceList.addAll(dynamicHealthResources.values());
        if (resourceList.size() != (healthResources.size() + dynamicHealthResources.size())) {
            throw new IllegalStateException("Something is wrong with comparator. Elements are lost!");
        }
        return new ArrayList<>(resourceList);
    }

    private List<Class<? extends HealthResource>> loadHealthResources() {
        List<Class<? extends HealthResource>> classes = new ArrayList<>();
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages("io.descoped.dc")
                .scan()
        ) {
            ClassInfoList classInfoList = scanResult.getClassesImplementing(HealthResource.class.getName());
            classInfoList.forEach(ci -> {
                classes.add(ci.loadClass(HealthResource.class));
            });
        }
        return classes;
    }

    private <R extends HealthResource> R createHealthResource(Class<R> healthResourceClass) {
        try {
            return healthResourceClass.getDeclaredConstructor().newInstance();

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new ApplicationException(e);
        }
    }

    private <R extends HealthResource> R createHealthResource(UUID id, Class<R> healthResourceClass) {
        try {
            Constructor<R> constructor = healthResourceClass.getDeclaredConstructor(UUID.class);
            return constructor.newInstance(id);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new ApplicationException(e);
        }
    }

    private Map<Class<? extends HealthResource>, HealthResource> createHealthResources(List<Class<? extends HealthResource>> loadedHealthResources) {
        Map<Class<? extends HealthResource>, HealthResource> healthResourceMap = new LinkedHashMap<>();

        for (Class<? extends HealthResource> resourceClass : loadedHealthResources) {
            // do not eager instantiate classes marked with excluded
            if (resourceClass.isAnnotationPresent(HealthResourceExclude.class)) {
                continue;
            }

            HealthResource healthResource = createHealthResource(resourceClass);
            healthResourceMap.put(resourceClass, healthResource);
        }

        return healthResourceMap;
    }

    /**
     * Creates a resource and adds it under an id.
     *
     * @param id                         the id
     * @param healthDynamicResourceClass the resource's class
     * @param <R>                        the resource's type
     * @return the resource
     */
    public <R extends HealthResource> R createAndAdddHealthResource(UUID id, Class<R> healthDynamicResourceClass) {
        HealthResource healthResource = createHealthResource(id, healthDynamicResourceClass);
        dynamicHealthResources.put(id, healthResource);
        return healthDynamicResourceClass.cast(healthResource);
    }

    /**
     * The resource added under an id.
     *
     * @param id  the id
     * @param <R> the resource's type
     * @return the resource, or {@code null}
     */
    @SuppressWarnings("unchecked") // the caller names the type it registered under the id
    public <R extends HealthResource> R getHealthResource(UUID id) {
        return (R) dynamicHealthResources.get(id);
    }

    /**
     * Removes the resource added under an id.
     *
     * @param id the id
     */
    public void removeHealthResource(UUID id) {
        dynamicHealthResources.remove(id);
    }

    private static class HealthResourcePriorityComparator implements Comparator<HealthResource> {

        @Override
        public int compare(HealthResource o1, HealthResource o2) {
            if (!o1.getClass().isAnnotationPresent(HealthRenderPriority.class)) {
                throw new ApplicationException("HealthResource " + o1 + " must be annotated with " + HealthRenderPriority.class.getName());
            }

            if (!o2.getClass().isAnnotationPresent(HealthRenderPriority.class)) {
                throw new ApplicationException("HealthResource " + o2 + " must be annotated with " + HealthRenderPriority.class.getName());
            }

            int o1Priority = o1.getClass().getAnnotation(HealthRenderPriority.class).priority();
            int o2Priority = o2.getClass().getAnnotation(HealthRenderPriority.class).priority();

            if (o1Priority == o2Priority) {
                return 1;
            }
            return o1Priority < o2Priority ? -1 : 1;
        }
    }

}
