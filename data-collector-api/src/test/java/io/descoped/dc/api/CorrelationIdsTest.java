package io.descoped.dc.api;

import io.descoped.dc.api.context.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationIdsTest {

    @Test
    void thatCorrelationIdIsBoundAndUnboundFromContext() {
        ExecutionContext context = ExecutionContext.empty();
        CorrelationIds cid = CorrelationIds.of(context);
        UUID uuid = cid.add();
        assertEquals(cid.first(), uuid);
        assertEquals(cid.last(), uuid);
    }

    @Test
    void thatAnInterruptedGetKeepsTheInterrupt() {
        CorrelationIds cid = CorrelationIds.create(ExecutionContext.empty());
        cid.add();
        Thread.currentThread().interrupt();
        assertThrows(RuntimeException.class, cid::get);
        assertTrue(Thread.interrupted(), "the interrupt is restored");
    }
}
