package io.descoped.dc.application.server;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.Set;

class ServiceProviderDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceProviderDiscovery.class);

    private ServiceProviderDiscovery() {
    }

    @SuppressWarnings("unchecked") // the class named in the service file implements T
    static <T> Class<T> classForName(String clazz) {
        try {
            return (Class<T>) Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new ApplicationException(e);
        }
    }

    static <T> Iterable<Class<T>> discover(Class<T> serviceProviderClass) {
        Set<Class<T>> serviceList = new LinkedHashSet<>();
        try (ScanResult scanResult = new ClassGraph().enableClasspath().enableNonSystemModules().acceptPathsNonRecursive("META-INF/services").scan()) {
            LOG.trace("serviceProviderClass: {}", serviceProviderClass.getName());
            // as ClassGraph's deprecated forEachByteArray: load, accept and close each resource
            for (Resource res : scanResult.getResourcesWithLeafName(serviceProviderClass.getName())) {
                byte[] content = load(res);
                try (BufferedReader reader = new BufferedReader(new StringReader(new String(content)))) {
                    reader.lines().forEach(clazz -> serviceList.add(classForName(clazz)));
                } catch (IOException e) {
                    throw new ApplicationException(e);
                }
            }
        }
        return serviceList;
    }

    // the resource's content, the resource closed; as ClassGraph's deprecated forEachByteArray
    static byte[] load(Resource resource) {
        try (resource) {
            return resource.load();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load resource " + resource, e);
        }
    }
}
