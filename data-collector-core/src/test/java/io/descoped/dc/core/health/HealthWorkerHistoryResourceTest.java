package io.descoped.dc.core.health;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.content.HealthContentStreamMonitor;
import io.descoped.dc.api.ids.TimeOrderedIds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthWorkerHistoryResourceTest {

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
