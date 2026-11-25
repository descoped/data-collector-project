package io.descoped.dc.test.server;

import io.descoped.dc.test.client.TestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(TestServerExtension.class)
public class CaseOneTest {

    @Inject
    TestClient client;

    @Test
    void thatExtensionIsInvoked() {
        System.out.println("Hello 1");
        assertNotNull(client);
    }
}
