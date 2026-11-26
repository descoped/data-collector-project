package io.descoped.dc.test.server;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.engine.discovery.ClassNameFilter.excludeClassNamePatterns;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

class TestServerExecutionListenerTest {

    @Test
    void thatConfigurationFactoryDiscoversAllTestConfigurations() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectPackage(CaseOneTest.class.getPackageName()),
                        selectClass(CaseOneTest.class),
                        selectClass(CaseTwoTest.class)
                )
                .filters(
                        includeClassNamePatterns(".*Case(\\w+)Test$"),
                        excludeClassNamePatterns("." + TestServerExecutionListenerTest.class.getSimpleName())
                )
                .build();

        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);

        /*
         * Invocation of TestServerExecutionListener.testPlanExecutionStarted triggers the ContainerFactory and
         * TestServerFactory to be initialized.
         */

        ConfigurationFactory configurationFactory = new ConfigurationFactory();
        TestServerFactory testServerFactory = new TestServerFactory(configurationFactory);
        TestServerExecutionListener testServerExecutionListener = new TestServerExecutionListener(configurationFactory, testServerFactory);

        testServerExecutionListener.testPlanExecutionStarted(testPlan);

        assertNotNull(configurationFactory);

        assertEquals(3, configurationFactory.keys().size());

        TestConfigurationBinding methodOverrideProfileKey = TestConfigurationBinding.method(CaseTwoTest.class, "thatMethodOverrideConfigurationIsCreated");
        Map<String, String> methodOverrideProfileMap = configurationFactory.get(methodOverrideProfileKey).asMap();

        assertEquals("bar", methodOverrideProfileMap.get("foo"));

    }

    @Test
    void testConfigurationOverrideKeyValuePairs() {
        String[] configurationOverrideValues = {"k1", "v1", "k2"};
        ConfigurationFactory configurationFactory = new ConfigurationFactory();
        TestServerFactory testServerFactory = new TestServerFactory(configurationFactory);
        TestServerExecutionListener listener = new TestServerExecutionListener(configurationFactory, testServerFactory);
        Map<String, String> overrides = new LinkedHashMap<>();
        MethodSource methodSource = MethodSource.from("Foo", "method");
        assertThrows(IllegalArgumentException.class, () -> listener.resolveOverrideKeyValueMap(configurationOverrideValues, overrides, methodSource));
    }

    @Test
    void thatReflectionFindMethodsByName() {
        TestServerExecutionListener listener = new TestServerExecutionListener(null, null);
        List<Method> methods = listener.findMethodsByName(CaseOneTest.class, MethodSource.from(CaseOneTest.class.getName(), "thatExtensionIsInvoked"));
        assertEquals(1, methods.size());

        MethodSource missingMethod = MethodSource.from(CaseOneTest.class.getName(), "methodDoesNotExist");
        assertThrows(IllegalStateException.class, () -> listener.findMethodsByName(CaseOneTest.class, missingMethod));
    }

    @Test
    void testLoadTestClassException() {
        LoadTestClassException loadTestClassException = new LoadTestClassException(new ClassNotFoundException());
        assertThrows(LoadTestClassException.class, () -> {
            throw loadTestClassException;
        });

        TestServerExecutionListener listener = new TestServerExecutionListener(null, null);
        RuntimeException runtimeException = new RuntimeException();
        assertSame(runtimeException, listener.handleException(runtimeException));

        assertInstanceOf(LoadTestClassException.class, listener.handleException(new ClassNotFoundException()));
    }
}
