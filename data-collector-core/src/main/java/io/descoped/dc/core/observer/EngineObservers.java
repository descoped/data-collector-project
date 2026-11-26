package io.descoped.dc.core.observer;

import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.Identity;
import io.descoped.dc.api.node.NodeWithId;
import io.descoped.dc.api.observer.ContentEvent;
import io.descoped.dc.api.observer.EngineObserver;
import io.descoped.dc.api.observer.NodeEvent;
import io.descoped.dc.api.observer.Observation;
import io.descoped.dc.api.observer.RequestEvent;
import io.descoped.dc.api.observer.RequestObservation;
import io.descoped.dc.api.observer.WorkerEvent;
import io.descoped.dc.core.http.URLInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The engine observers found with {@link ServiceLoader}, as one observer. Every call reaches every observer, in the
 * order of their class names. A {@link RuntimeException} an observer throws is logged once for that observer and event,
 * with its class only, and does not reach the engine; an {@link Error} does.
 */
public final class EngineObservers implements EngineObserver {

    private static final Logger LOG = LoggerFactory.getLogger(EngineObservers.class);

    private static final EngineObservers INSTANCE = new EngineObservers(load());

    private final List<EngineObserver> observers;
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    EngineObservers(List<EngineObserver> observers) {
        this.observers = List.copyOf(observers);
    }

    /**
     * The observers of this JVM.
     *
     * @return the observers
     */
    public static EngineObservers instance() {
        return INSTANCE;
    }

    static List<EngineObserver> load() {
        List<EngineObserver> found = new ArrayList<>();
        ServiceLoader<EngineObserver> loader = ServiceLoader.load(EngineObserver.class, EngineObservers.class.getClassLoader());
        for (ServiceLoader.Provider<EngineObserver> provider : loader.stream().toList()) {
            try {
                found.add(provider.get());
            } catch (ServiceConfigurationError e) {
                LOG.warn("Engine observer {} could not be created: {}", provider.type().getName(), e.getClass().getName());
            }
        }
        found.sort(Comparator.comparing(observer -> observer.getClass().getName()));
        LOG.info("Engine observers: {}", found.stream().map(observer -> observer.getClass().getName()).toList());
        return found;
    }

    /**
     * The event of a node.
     *
     * @param node the node
     * @return its type and id
     */
    public static NodeEvent nodeEvent(Base node) {
        Class<?>[] interfaces = node.getClass().getInterfaces();
        String type = interfaces.length > 0 ? interfaces[0].getSimpleName() : node.getClass().getSimpleName();
        String id = switch (node) {
            case NodeWithId withId -> withId.id();
            case Identity identity -> identity.id();
            default -> null;
        };
        return new NodeEvent(type, id);
    }

    /**
     * The event of a request.
     *
     * @param url the request's URL, or {@code null}
     * @return its location, empty when the URL is not known or cannot be read
     */
    public static RequestEvent requestEvent(String url) {
        try {
            return new RequestEvent(new URLInfo(url).getLocation().orElse(""));
        } catch (RuntimeException _) {
            return new RequestEvent("");
        }
    }

    @Override
    public void workerStarted(WorkerEvent worker) {
        each("workerStarted", observer -> observer.workerStarted(worker));
    }

    @Override
    public void workerFinished(WorkerEvent worker, String status) {
        each("workerFinished", observer -> observer.workerFinished(worker, status));
    }

    @Override
    public Observation nodeStarted(NodeEvent node) {
        return start("nodeStarted", observer -> observer.nodeStarted(node));
    }

    @Override
    public RequestObservation requestStarted(RequestEvent request) {
        return start("requestStarted", observer -> observer.requestStarted(request));
    }

    @Override
    public void requestRetried(RequestEvent request, int attempt) {
        each("requestRetried", observer -> observer.requestRetried(request, attempt));
    }

    @Override
    public void operationFailed(RequestEvent request, Throwable failure) {
        each("operationFailed", observer -> observer.operationFailed(request, failure));
    }

    @Override
    public void pageCompleted(NodeEvent paginate) {
        each("pageCompleted", observer -> observer.pageCompleted(paginate));
    }

    @Override
    public void entryFannedOut(NodeEvent parallel) {
        each("entryFannedOut", observer -> observer.entryFannedOut(parallel));
    }

    @Override
    public Observation addingPage(ContentEvent content) {
        return start("addingPage", observer -> observer.addingPage(content));
    }

    @Override
    public void entryBuffered(ContentEvent content) {
        each("entryBuffered", observer -> observer.entryBuffered(content));
    }

    @Override
    public void documentBuffered(ContentEvent content) {
        each("documentBuffered", observer -> observer.documentBuffered(content));
    }

    @Override
    public Observation publishing(ContentEvent content, int positions) {
        return start("publishing", observer -> observer.publishing(content, positions));
    }

    private void each(String event, Consumer<EngineObserver> call) {
        for (EngineObserver observer : observers) {
            try {
                call.accept(observer);
            } catch (RuntimeException e) {
                warn(observer, event, e);
            }
        }
    }

    private Observations start(String event, Function<EngineObserver, ? extends Observation> call) {
        List<Started> started = new ArrayList<>(observers.size());
        for (EngineObserver observer : observers) {
            try {
                Observation observation = call.apply(observer);
                if (observation != null && observation != Observation.NONE && observation != RequestObservation.NONE) {
                    started.add(new Started(observer, event, observation));
                }
            } catch (RuntimeException e) {
                warn(observer, event, e);
            }
        }
        return new Observations(started);
    }

    void warn(EngineObserver observer, String event, RuntimeException e) {
        String name = observer.getClass().getName();
        if (warned.add(name + "#" + event)) {
            LOG.warn("Engine observer {} failed on {}: {}", name, event, e.getClass().getName());
        }
    }

    private record Started(EngineObserver observer, String event, Observation observation) {
    }

    // the observations of one event, each called safely
    private final class Observations implements RequestObservation {

        private final List<Started> started;

        private Observations(List<Started> started) {
            this.started = started;
        }

        @Override
        public void responded(int statusCode, boolean emptyResponse) {
            each(observation -> {
                if (observation instanceof RequestObservation request) {
                    request.responded(statusCode, emptyResponse);
                }
            });
        }

        @Override
        public void failed(Throwable failure) {
            each(observation -> observation.failed(failure));
        }

        @Override
        public void close() {
            each(Observation::close);
        }

        private void each(Consumer<Observation> call) {
            for (Started one : started) {
                try {
                    call.accept(one.observation());
                } catch (RuntimeException e) {
                    warn(one.observer(), one.event(), e);
                }
            }
        }
    }
}
