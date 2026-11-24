package io.descoped.dc.api.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectCreatorTest {

    public static class Dependency {
    }

    public static class WithDependency {
        final Dependency dependency;

        public WithDependency(Dependency dependency) {
            this.dependency = dependency;
        }
    }

    public static class WithPrivateConstructor {
        private WithPrivateConstructor() {
        }
    }

    public static class WithUnknownDependency {
        public WithUnknownDependency(String unknown) {
        }
    }

    @Test
    void thatConstructorParametersAreInjectedFromServices() {
        Dependency dependency = new Dependency();
        Services services = Services.create().register(Dependency.class, dependency);
        WithDependency instance = ObjectCreator.newInstance(WithDependency.class, services);
        assertSame(dependency, instance.dependency);
    }

    @Test
    void thatTheDefaultConstructorIsUsedWithoutAPublicOne() {
        assertNotNull(ObjectCreator.newInstance(WithPrivateConstructor.class, Services.create()));
    }

    @Test
    void thatAParameterNotInServicesIsRefused() {
        Services services = Services.create();
        assertThrows(IllegalStateException.class, () -> ObjectCreator.newInstance(WithUnknownDependency.class, services));
    }
}
