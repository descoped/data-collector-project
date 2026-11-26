package io.descoped.dc.core.health;

import io.descoped.dc.api.content.HealthContentStreamMonitor;
import io.descoped.dc.core.executor.WorkerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthWorkerHistoryResourceTest {

    // a failed assertion must not leave the flag set for the next test
    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    void thatAddedWorkerIsInTheHistory() {
        HealthWorkerHistoryResource history = new HealthWorkerHistoryResource();
        HealthWorkerResource worker = new HealthWorkerResource(UUID.randomUUID());
        worker.getMonitor().contentStream().setMonitor(new HealthContentStreamMonitor(() -> true, () -> 0, () -> 0));

        history.add(worker);

        assertEquals(1, ((List<?>) history.resource()).size());
    }

    @Test
    void thatInterruptedAddRestoresTheInterrupt() {
        HealthWorkerHistoryResource history = new HealthWorkerHistoryResource();
        HealthWorkerResource worker = new HealthWorkerResource(UUID.randomUUID());

        Thread.currentThread().interrupt();
        assertThrows(WorkerException.class, () -> history.add(worker));

        assertTrue(Thread.interrupted());
        assertEquals(0, ((List<?>) history.resource()).size());
    }
}
