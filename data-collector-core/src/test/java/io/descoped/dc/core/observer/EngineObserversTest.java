package io.descoped.dc.core.observer;

import io.descoped.dc.api.observer.EngineObserver;
import io.descoped.dc.api.observer.NodeEvent;
import io.descoped.dc.api.observer.Observation;
import io.descoped.dc.api.observer.RequestEvent;
import io.descoped.dc.api.observer.RequestObservation;
import io.descoped.dc.api.observer.WorkerEvent;
import io.descoped.dc.core.metrics.PrometheusEngineObserver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineObserversTest {

    // an observer that fails on every call, before and after an observer that records
    static final class FailingObserver implements EngineObserver {
        @Override
        public void workerStarted(WorkerEvent worker) {
            throw new IllegalStateException("worker");
        }

        @Override
        public Observation nodeStarted(NodeEvent node) {
            return new Observation() {
                @Override
                public void failed(Throwable failure) {
                    throw new IllegalStateException("failed");
                }

                @Override
                public void close() {
                    throw new IllegalStateException("close");
                }
            };
        }

        @Override
        public RequestObservation requestStarted(RequestEvent request) {
            throw new IllegalStateException("request");
        }
    }

    static final class RecordingObserver implements EngineObserver {
        final List<String> calls = new ArrayList<>();

        @Override
        public void workerStarted(WorkerEvent worker) {
            calls.add("workerStarted");
        }

        @Override
        public Observation nodeStarted(NodeEvent node) {
            calls.add("nodeStarted " + node.type());
            return new Observation() {
                @Override
                public void failed(Throwable failure) {
                    calls.add("failed");
                }

                @Override
                public void close() {
                    calls.add("close");
                }
            };
        }

        @Override
        public RequestObservation requestStarted(RequestEvent request) {
            calls.add("requestStarted " + request.location());
            return new RequestObservation() {
                @Override
                public void responded(int statusCode, boolean emptyResponse) {
                    calls.add("responded " + statusCode);
                }
            };
        }
    }

    @Test
    void thatAFailingObserverNeverReachesTheEngineNorTheOtherObservers() {
        RecordingObserver recording = new RecordingObserver();
        EngineObservers observers = new EngineObservers(List.of(new FailingObserver(), recording));

        assertDoesNotThrow(() -> {
            observers.workerStarted(new WorkerEvent(UUID.randomUUID(), "specification", "name"));
            Observation node = observers.nodeStarted(new NodeEvent("Get", "page"));
            node.failed(new IllegalStateException("the node failed"));
            node.close();
            RequestObservation request = observers.requestStarted(new RequestEvent("http://localhost"));
            request.responded(200, false);
            request.close();
        });

        assertEquals(List.of("workerStarted", "nodeStarted Get", "failed", "close", "requestStarted http://localhost", "responded 200"), recording.calls);
    }

    @Test
    void thatTheEnginesObserversAreFound() {
        assertTrue(EngineObservers.load().stream().anyMatch(PrometheusEngineObserver.class::isInstance));
    }

    @Test
    void thatAnUnreadableUrlHasNoLocation() {
        assertEquals("", EngineObservers.requestEvent("not a url").location());
        assertEquals("", EngineObservers.requestEvent(null).location());
        assertEquals("https://example.org", EngineObservers.requestEvent("https://user:secret@example.org/path?token=x").location());
    }
}
