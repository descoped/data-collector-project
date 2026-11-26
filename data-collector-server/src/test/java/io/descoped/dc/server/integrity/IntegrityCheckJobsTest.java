package io.descoped.dc.server.integrity;

import io.descoped.dc.server.integrity.IntegrityCheckService.JobStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrityCheckJobsTest {

    @Test
    void thatEachCompletedJobIsListedOnce() {
        // a finished job still held in memory, and two completed jobs on disk, that one among them
        List<JobStatus> inMemory = List.of(new JobStatus("topic-b", "CLOSED"));
        List<JobStatus> onDisk = List.of(new JobStatus("topic-a", "CLOSED"), new JobStatus("topic-b", "CLOSED"));
        assertEquals(List.of(new JobStatus("topic-b", "CLOSED"), new JobStatus("topic-a", "CLOSED")),
                IntegrityCheckService.merge(inMemory, onDisk));
        assertEquals(onDisk, IntegrityCheckService.merge(List.of(), onDisk));
    }
}
