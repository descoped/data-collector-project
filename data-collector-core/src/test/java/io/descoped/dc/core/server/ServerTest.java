package io.descoped.dc.core.server;

import io.descoped.dc.test.client.TestClient;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import io.undertow.Undertow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TestServerExtension.class)
class ServerTest {

    @Inject
    TestServer server;

    @Inject
    TestClient client;

    @Test
    void thatTestServerStartsMainWithRunningUndertowListener() {
        List<Undertow.ListenerInfo> listenerInfo = server.getApplication().unwrap(Undertow.class).getListenerInfo();
        Undertow.ListenerInfo info = listenerInfo.iterator().next();
        assertEquals("http", info.getProtcol());
    }

    @Test
    void testTestController() {
        client.get("/test").expect200Ok();
    }

    @Test
    void testLoopbackController() {
        System.out.println(client.get("/echo").expect200Ok().body());
    }
}
