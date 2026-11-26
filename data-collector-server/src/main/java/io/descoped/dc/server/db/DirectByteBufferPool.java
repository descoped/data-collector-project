package io.descoped.dc.server.db;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A fixed pool of direct byte buffers, used by one thread: an integrity check's index, whose LMDB writes are tied to
 * one thread as well. No other thread releases a buffer, so an empty pool fails at once instead of waiting (#68).
 */
public class DirectByteBufferPool implements AutoCloseable {

    final Deque<ByteBuffer> deque;

    /**
     * Allocates the buffers.
     *
     * @param n        the number of buffers
     * @param capacity each buffer's capacity
     */
    public DirectByteBufferPool(int n, int capacity) {
        deque = new ArrayDeque<>(n);
        for (int i = 0; i < n; i++) {
            deque.addLast(ByteBuffer.allocateDirect(capacity));
        }
    }

    /**
     * Takes a buffer.
     *
     * @return the buffer
     * @throws IllegalStateException if every buffer is taken
     */
    public ByteBuffer acquire() {
        ByteBuffer buffer = deque.pollFirst();
        if (buffer == null) {
            throw new IllegalStateException("Unable to acquire KeyBuffer!");
        }
        return buffer;
    }

    /**
     * Returns a buffer to the pool, cleared.
     *
     * @param buffer the buffer
     */
    public void release(ByteBuffer buffer) {
        deque.addLast(buffer.clear());
    }

    @Override
    public void close() {
        deque.clear();
    }
}
