package io.descoped.dc.core.health;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.content.HealthContentStreamMonitor;
import io.descoped.dc.api.ids.TimeOrderedIds;
import io.descoped.dc.core.executor.WorkerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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

    // #56: the history keeps the latest 25 workers, newest first
    @Test
    void thatTheHistoryKeepsTheLatestWorkers() {
        HealthWorkerHistoryResource history = new HealthWorkerHistoryResource();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            UUID workerId = TimeOrderedIds.generate();
            ids.add(workerId.toString());
            HealthWorkerResource worker = new HealthWorkerResource(workerId);
            // as Worker sets it from its content store
            worker.getMonitor().contentStream().setMonitor(new HealthContentStreamMonitor(() -> false, () -> 0, () -> 0));
            history.add(worker);
        }

        @SuppressWarnings("unchecked")
        List<ObjectNode> shown = (List<ObjectNode>) history.resource();
        assertEquals(25, shown.size());
        assertEquals(ids.get(29), shown.get(0).get("worker-id").asText());
        assertEquals(ids.get(5), shown.get(24).get("worker-id").asText());
    }
}
