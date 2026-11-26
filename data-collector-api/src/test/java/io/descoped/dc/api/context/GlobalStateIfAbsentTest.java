package io.descoped.dc.api.context;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalStateIfAbsentTest {

    @Test
    void thatTheValueIsStoredOnceAndReturned() {
        ExecutionContext context = ExecutionContext.empty();
        AtomicInteger calls = new AtomicInteger();
        String first = context.globalStateIfAbsent("key", () -> "value-" + calls.incrementAndGet());
        String second = context.globalStateIfAbsent("key", () -> "value-" + calls.incrementAndGet());
        assertEquals("value-1", first);
        assertEquals("value-1", second);
        assertEquals(1, calls.get());
    }
}
