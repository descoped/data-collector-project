package io.descoped.dc.application.metrics;

import io.descoped.dc.api.metrics.MetricsResource;
import io.descoped.dc.application.server.ApplicationException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates and holds the discovered metrics resources.
 */
public class MetricsResourceFactory {

    private final List<Class<? extends MetricsResource>> metricsResourceClasses;
    private final Map<Class<? extends MetricsResource>, ? extends MetricsResource> metricsResources;

    /**
     * Creates the factory with the discovered resources.
     */
    public MetricsResourceFactory() {
        metricsResourceClasses = loadMetricsResources();
        metricsResources = createMetricsResources(metricsResourceClasses);
    }

    /**
     * Creates the factory.
     *
     * @return the factory
     */
    public static MetricsResourceFactory create() {
        return new MetricsResourceFactory();
    }

    private List<Class<? extends MetricsResource>> loadMetricsResources() {
        List<Class<? extends MetricsResource>> classes = new ArrayList<>();
        // ClassGraph 6 scans only the sources it is given: the class path and the module path, as 4.x did
        try (ScanResult scanResult = new ClassGraph()
                .enableClasspath()
                .enableNonSystemModules()
                .enableClassInfo()
                .acceptPackages("io.descoped.dc")
                .scan()
        ) {
            ClassInfoList classInfoList = scanResult.getAllClassesImplementing(MetricsResource.class.getName());
            // ClassGraph 6 does not load classes; these are dc's own, found by the scan
            for (ClassInfo ci : classInfoList) {
                classes.add(loadClass(ci.getName()));
            }
        }
        return classes;
    }

    private <R extends MetricsResource> R createMetricsResource(Class<R> metricsResourceClass) {
        try {
            return metricsResourceClass.getDeclaredConstructor().newInstance();

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new ApplicationException(e);
        }
    }


    private Map<Class<? extends MetricsResource>, ? extends MetricsResource> createMetricsResources(List<Class<? extends MetricsResource>> loadedMetricsResources) {
        Map<Class<? extends MetricsResource>, MetricsResource> metricsResourceMap = new LinkedHashMap<>();

        for (Class<? extends MetricsResource> resourceClass : loadedMetricsResources) {
            MetricsResource metricsResource = createMetricsResource(resourceClass);
            metricsResourceMap.put(resourceClass, metricsResource);
        }

        return metricsResourceMap;
    }

    /**
     * The resource of a class.
     *
     * @param metricsResourceClass the class
     * @param <R>                  the resource's type
     * @return the resource, or {@code null}
     */
    public <R extends MetricsResource> R get(Class<R> metricsResourceClass) {
        return metricsResourceClass.cast(metricsResources.get(metricsResourceClass));
    }

    // the factory's loader: dc's modules share the application loader on the module path and the class path
    private static Class<? extends MetricsResource> loadClass(String name) {
        try {
            return Class.forName(name, false, MetricsResourceFactory.class.getClassLoader()).asSubclass(MetricsResource.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load " + name, e);
        }
    }
}
