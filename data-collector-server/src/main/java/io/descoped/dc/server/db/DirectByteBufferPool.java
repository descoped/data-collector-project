package io.descoped.dc.server.db;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * A fixed pool of direct byte buffers.
 */
public class DirectByteBufferPool implements AutoCloseable {

    final BlockingQueue<ByteBuffer> deque;

    /**
     * Allocates the buffers.
     *
     * @param n        the number of buffers
     * @param capacity each buffer's capacity
     */
    public DirectByteBufferPool(int n, int capacity) {
        deque = new LinkedBlockingDeque<>(n);
        for (int i = 0; i < n; i++) {
            try {
                deque.put(ByteBuffer.allocateDirect(capacity));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Takes a buffer, waiting for one to be released.
     *
     * @return the buffer
     * @throws IllegalStateException if none is released in time
     */
    public ByteBuffer acquire() {
        try {
            int failCount = 25;
            ByteBuffer buffer;
            while ((buffer = deque.poll(1, TimeUnit.SECONDS)) == null) {
                if (failCount == 0) {
                    throw new IllegalStateException("Unable to acquire KeyBuffer!");
                }
                TimeUnit.MILLISECONDS.sleep(50);
                failCount--;
            }
            return buffer;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a buffer to the pool.
     *
     * @param buffer the buffer
     */
    public void release(ByteBuffer buffer) {
        try {
            deque.put(buffer.clear());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        deque.clear();
    }
}
