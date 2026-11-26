package io.descoped.dc.core.handler;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A retried request gets a new handler of the same file: the abandoned attempt is cancelled and writes no more, and
 * the file holds only the new attempt's body.
 */
class FileBodyHandlerRetryTest {

    static class TestSubscription implements Flow.Subscription {
        final AtomicBoolean cancelled = new AtomicBoolean();

        @Override
        public void request(long n) {
            // the test pushes the buffers itself
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }
    }

    static List<ByteBuffer> bytes(String text) {
        return List.of(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void thatARetryWritesTheFileFromTheStart() throws Exception {
        TempFileBodyHandler first = TempFileBodyHandler.ofFile();
        Path file = first.getFile();
        try {
            assertTrue(first.claim());
            TestSubscription firstSubscription = new TestSubscription();
            first.onSubscribe(firstSubscription);
            first.onNext(bytes("partial "));

            assertFalse(first.claim(), "a handler receives one request's body");
            FileBodyHandler second = first.restart();
            assertInstanceOf(TempFileBodyHandler.class, second);
            assertTrue(firstSubscription.cancelled.get());
            assertEquals(file, second.getFile());
            assertEquals("", Files.readString(file));

            first.onNext(bytes("late "));
            TestSubscription lateSubscription = new TestSubscription();
            first.onSubscribe(lateSubscription);
            assertTrue(lateSubscription.cancelled.get(), "the abandoned handler refuses a late subscription");

            assertTrue(second.claim());
            second.onSubscribe(new TestSubscription());
            second.onNext(bytes("complete body"));
            second.onComplete();
            first.onNext(bytes("later"));
            assertEquals("complete body", Files.readString(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
