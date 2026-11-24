package io.descoped.dc.core.handler;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TempFileBodyHandlerTest {

    @Test
    void thatTempFileIsForItsOwnerOnly() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"), "needs POSIX permissions");

        TempFileBodyHandler bodyHandler = TempFileBodyHandler.ofFile();
        try {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(bodyHandler.body())));
        } finally {
            bodyHandler.dispose();
        }
    }

    @Test
    void thatDisposeRemovesTheTempFile() {
        TempFileBodyHandler bodyHandler = TempFileBodyHandler.ofFile();
        Path file = bodyHandler.body();
        assertTrue(Files.exists(file));

        bodyHandler.dispose();

        assertFalse(Files.exists(file));
        assertThrows(IllegalStateException.class, bodyHandler::dispose);
    }

    @Test
    void thatASecondSubscriptionIsCancelled() {
        TempFileBodyHandler bodyHandler = TempFileBodyHandler.ofFile();
        try {
            RecordingSubscription first = new RecordingSubscription();
            RecordingSubscription second = new RecordingSubscription();

            bodyHandler.onSubscribe(first);
            bodyHandler.onSubscribe(second);

            assertEquals(Long.MAX_VALUE, first.requested);
            assertFalse(first.cancelled);
            assertTrue(second.cancelled);
        } finally {
            bodyHandler.dispose();
        }
    }

    @Test
    void thatErrorIsRethrownUnchecked() {
        TempFileBodyHandler bodyHandler = TempFileBodyHandler.ofFile();
        try {
            IllegalStateException runtimeException = new IllegalStateException("failed");
            assertSame(runtimeException, assertThrows(IllegalStateException.class, () -> bodyHandler.onError(runtimeException)));

            IOException checkedException = new IOException("failed");
            assertSame(checkedException, assertThrows(RuntimeException.class, () -> bodyHandler.onError(checkedException)).getCause());
        } finally {
            bodyHandler.dispose();
        }
    }

    static class RecordingSubscription implements Flow.Subscription {
        long requested;
        boolean cancelled;

        @Override
        public void request(long n) {
            requested += n;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
