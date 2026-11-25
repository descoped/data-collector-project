package io.descoped.dc.test.server;

import io.descoped.dc.test.client.TestClient;
import io.descoped.dc.test.config.ConfigurationOverride;
import io.descoped.dc.test.config.ConfigurationProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(TestServerExtension.class)
class CaseTwoTest {

    @Inject
    TestClient client;

    @Test
    void thatExtensionIsInvoked() {
        System.out.println("Hello 2");
        assertNotNull(client);
    }

    @ConfigurationOverride({"foo", "bar"})
    @Test
    void thatMethodOverrideConfigurationIsCreated() {
        // the method's configuration is checked in TestServerExecutionListenerTest
        assertNotNull(client);
    }

    @ConfigurationProfile("dummy")
    @ConfigurationOverride({"foo", "bar"})
    @Test
    void thatMethodOverrideProfileConfigurationIsCreated() {
        // the method's configuration is checked in TestServerExecutionListenerTest
        assertNotNull(client);
    }

    @ConfigurationProfile("dummy")
    @ConfigurationOverride({"foo", "bar"})
    @Test
    void thatMethodOverrideProfileConfiguration2IsCreated() {
        // the method's configuration is checked in TestServerExecutionListenerTest
        assertNotNull(client);
    }

    @Test
    void handleException() {
        assertThrows(RuntimeException.class, () -> {
            throw new RuntimeException("Blow!");
        });
    }
}
