package io.descoped.dc.test.server;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.test.client.TestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Creates and keeps the test servers of a test plan, one for each configuration binding.
 */
public class TestServerFactory {

    private final ConfigurationFactory configurationFactory;
    private final Map<TestConfigurationBinding, Supplier<TestServer>> testServerSuppliers = new ConcurrentHashMap<>();

    /*
     * The TestServerExtension maintains current execution test server target in beforeTestExecution
     */
    private final AtomicReference<TestServerExtension.TestServerResource> currentTestServerResourceReference = new AtomicReference<>();

    TestServerFactory(ConfigurationFactory configurationFactory) {
        this.configurationFactory = configurationFactory;
    }

    /**
     * The factory of the running test plan.
     *
     * @return the factory
     */
    public static TestServerFactory instance() {
        return TestServerFactory.TestServerFactorySingleton.INSTANCE;
    }

    State state(TestConfigurationBinding testConfigurationBinding) {
        if (testConfigurationBinding.isContainer()) {
            return State.CONTAINER;

        } else if (testConfigurationBinding.isClass()) {
            return State.CLASS;

        } else if (testConfigurationBinding.isMethod()) {
            return State.METHOD;

        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Create TestServer suppliers for lazy initialization/creation of test server instances.
     * A TestServer is bound to a TestConfigurationBinding that is resolved by the TestServerExtension during
     * test execution.
     */
    void initialize() {
        createTestServerForConfigurationBinding(TestConfigurationBinding.container());

        for (TestConfigurationBinding testConfigurationBinding : configurationFactory.keys()) {
            createTestServerForConfigurationBinding(testConfigurationBinding);
        }
    }

    /**
     * Resolve the nearest test configuration binding in the order: method, class or container.
     *
     * @param testConfigurationBinding test method target
     * @return if the test-method is annotated it will try to find class config, or fallback to the container config
     */
    TestConfigurationBinding findFallbackConfiguration(TestConfigurationBinding testConfigurationBinding) {
        // return bound configuration
        if (testServerSuppliers.containsKey(testConfigurationBinding)) {
            return testConfigurationBinding;
        }

        // fallback method to class level configuration if exists
        if (testConfigurationBinding.isMethod()) {
            TestConfigurationBinding classTestConfigurationBinding = TestConfigurationBinding.clazz(testConfigurationBinding.getClassName());
            if (testServerSuppliers.containsKey(classTestConfigurationBinding)) {
                return classTestConfigurationBinding;
            }
        }

        // fallback to default configuration if none matches
        return TestConfigurationBinding.container();
    }

    private void createTestServerForConfigurationBinding(TestConfigurationBinding testConfigurationBinding) {
        Supplier<TestServer> testServerSupplier = new Supplier<>() {
            final DynamicConfiguration configuration = configurationFactory.get(testConfigurationBinding);

            @Override
            public TestServer get() {
                return new TestServer(configuration, 0);
            }
        };

        testServerSuppliers.put(testConfigurationBinding, testServerSupplier);
    }

    Supplier<TestServer> getSupplier(TestConfigurationBinding testConfigurationBinding) {
        return testServerSuppliers.get(testConfigurationBinding);
    }

    // see field comment
    void setCurrentTestServerResource(TestServerExtension.TestServerResource testServerResource) {
        currentTestServerResourceReference.set(testServerResource);
    }

    /**
     * Returns the current server depending on scope.
     *
     * @return from {@code @BeforeAll}, always the container's instance; from {@code @BeforeEach}, the class's or the container's instance
     */
    public TestServer currentServer() {
        TestServerExtension.TestServerResource testServerResource = currentTestServerResourceReference.get();
        if (testServerResource == null) {
            throw new IllegalStateException("The test server has yet NOT been created!");
        }
        return testServerResource.getServer();
    }

    /**
     * Returns the current client depending on scope.
     *
     * @return from {@code @BeforeAll}, always the container's client; from {@code @BeforeEach}, the class's or the container's client
     */
    public TestClient currentClient() {
        TestServerExtension.TestServerResource testServerResource = currentTestServerResourceReference.get();
        if (testServerResource == null) {
            throw new IllegalStateException("The test server has yet NOT been created!");
        }
        return testServerResource.getClient();
    }

    /**
     * Where a test server is bound: to the whole test plan, a test class or a test method.
     */
    public enum State {
        /**
         * Bound to the test plan.
         */
        CONTAINER,
        /**
         * Bound to a test class.
         */
        CLASS,
        /**
         * Bound to a test method.
         */
        METHOD;

        static State of(TestConfigurationBinding key) {
            if (key.isContainer()) {
                return CONTAINER;
            } else if (key.isClass()) {
                return CLASS;
            } else if (key.isMethod()) {
                return METHOD;
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static class TestServerFactorySingleton {
        private static final TestServerFactory INSTANCE = new TestServerFactory(ConfigurationFactory.instance());
    }
}
