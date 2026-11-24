package io.descoped.dc.core.http;

import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.BodyHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpClientDelegateTest {

    // nothing listens on port 1 of the loopback address, so the connection is refused
    static Request refusedRequest() {
        return Request.newRequestBuilder().url("http://127.0.0.1:1/").GET().build();
    }

    // receives nothing: none of the requests here gets a response
    static class NoBodyHandler implements BodyHandler<Void> {
        @Override
        public Void body() {
            return null;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.cancel();
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            // no body is read
        }

        @Override
        public void onError(Throwable throwable) {
            // the request fails before a body arrives
        }

        @Override
        public void onComplete() {
            // no body is read
        }
    }

    @Test
    void thatRefusedConnectionIsRethrown() {
        Client client = Client.newClient();
        Request request = refusedRequest();

        RuntimeException e = assertThrows(RuntimeException.class, () -> client.send(request));
        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void thatRefusedConnectionIsRethrownWithBodyHandler() {
        Client client = Client.newClient();
        Request request = refusedRequest();
        NoBodyHandler bodyHandler = new NoBodyHandler();

        RuntimeException e = assertThrows(RuntimeException.class, () -> client.send(request, bodyHandler));
        assertInstanceOf(IOException.class, e.getCause());
    }

    // the client sends nothing on an interrupted thread; the delegate restores the interrupt flag
    @Test
    void thatInterruptIsRestored() {
        Client client = Client.newClient();
        Request request = refusedRequest();

        Thread.currentThread().interrupt();
        RuntimeException e = assertThrows(RuntimeException.class, () -> client.send(request));

        assertTrue(Thread.interrupted());
        assertInstanceOf(InterruptedException.class, e.getCause());
    }

    @Test
    void thatInterruptIsRestoredWithBodyHandler() {
        Client client = Client.newClient();
        Request request = refusedRequest();
        NoBodyHandler bodyHandler = new NoBodyHandler();

        Thread.currentThread().interrupt();
        RuntimeException e = assertThrows(RuntimeException.class, () -> client.send(request, bodyHandler));

        assertTrue(Thread.interrupted());
        assertInstanceOf(InterruptedException.class, e.getCause());
    }
}
