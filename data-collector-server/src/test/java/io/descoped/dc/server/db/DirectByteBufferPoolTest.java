package io.descoped.dc.server.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DirectByteBufferPoolTest {

    // no other thread releases a buffer, so an empty pool fails at once instead of after about 26 s (#68)
    @Test
    @Timeout(5)
    void thatAnEmptyPoolFailsAtOnce() {
        try (DirectByteBufferPool pool = new DirectByteBufferPool(1, 8)) {
            ByteBuffer buffer = pool.acquire();
            assertThrows(IllegalStateException.class, pool::acquire);
            pool.release(buffer);
            assertSame(buffer, pool.acquire());
        }
    }
}
