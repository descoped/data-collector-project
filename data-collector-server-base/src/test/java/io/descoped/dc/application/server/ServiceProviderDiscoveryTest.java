package io.descoped.dc.application.server;

import io.descoped.dc.application.controller.HealthController;
import io.descoped.dc.application.controller.MetricsController;
import io.descoped.dc.application.spi.Component;
import io.descoped.dc.application.spi.Controller;
import io.descoped.dc.application.spi.Service;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceProviderDiscoveryTest {

    @Test
    void testController() {
        Iterable<Class<Controller>> discovery = ServiceProviderDiscovery.discover(Controller.class);
        discovery.forEach(a -> System.out.println(a.getName()));
        List<String> names = new ArrayList<>();
        discovery.forEach(a -> names.add(a.getName()));
        assertTrue(names.containsAll(List.of(HealthController.class.getName(), MetricsController.class.getName())), () -> "missing controllers in " + names);
    }

    @Test
    void testService() {
        Iterable<Class<Service>> discovery = ServiceProviderDiscovery.discover(Service.class);
        discovery.forEach(a -> System.out.println(a.getName()));
        discovery.forEach(a -> assertTrue(Service.class.isAssignableFrom(a), a::getName));
    }

    @Test
    void testComponent() {
        Iterable<Class<Component>> discovery = ServiceProviderDiscovery.discover(Component.class);
        discovery.forEach(a -> System.out.println(a.getName()));
        discovery.forEach(a -> assertTrue(Component.class.isAssignableFrom(a), a::getName));
    }
}
