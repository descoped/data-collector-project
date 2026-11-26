package io.descoped.dc.core.http;

import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.observer.EngineObserver;
import io.descoped.dc.api.observer.RequestObservation;
import io.descoped.dc.core.observer.EngineObservers;

import java.util.concurrent.CompletableFuture;

/**
 * A client that reports each request to the engine observers: when it is sent, and when it is answered or fails.
 */
public final class ObservedClient implements Client {

    private final Client delegate;
    private final EngineObserver observer;

    ObservedClient(Client delegate, EngineObserver observer) {
        this.delegate = delegate;
        this.observer = observer;
    }

    /**
     * Observes a client with the engine observers.
     *
     * @param client the client
     * @return the observed client, or the client itself when it is observed already
     */
    public static Client of(Client client) {
        return client instanceof ObservedClient ? client : new ObservedClient(client, EngineObservers.instance());
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public Response send(Request request) {
        RequestObservation observation = started(request);
        try {
            Response response = delegate.send(request);
            responded(observation, response);
            return response;
        } catch (RuntimeException e) {
            observation.failed(e);
            throw e;
        } finally {
            observation.close();
        }
    }

    @Override
    public <R> Response send(Request request, BodyHandler<R> bodyHandler) {
        RequestObservation observation = started(request);
        try {
            Response response = delegate.send(request, bodyHandler);
            responded(observation, response);
            return response;
        } catch (RuntimeException e) {
            observation.failed(e);
            throw e;
        } finally {
            observation.close();
        }
    }

    @Override
    public CompletableFuture<Response> sendAsync(Request request) {
        RequestObservation observation = started(request);
        CompletableFuture<Response> future;
        try {
            future = delegate.sendAsync(request);
        } catch (RuntimeException e) {
            observation.failed(e);
            observation.close();
            throw e;
        }
        return observed(observation, future);
    }

    @Override
    public <R> CompletableFuture<Response> sendAsync(Request request, BodyHandler<R> bodyHandler) {
        RequestObservation observation = started(request);
        CompletableFuture<Response> future;
        try {
            future = delegate.sendAsync(request, bodyHandler);
        } catch (RuntimeException e) {
            observation.failed(e);
            observation.close();
            throw e;
        }
        return observed(observation, future);
    }

    @Override
    public Object getDelegate() {
        return delegate.getDelegate();
    }

    private RequestObservation started(Request request) {
        return observer.requestStarted(EngineObservers.requestEvent(request == null ? null : request.url()));
    }

    // the future is returned as the client gave it; the observation ends when it completes
    private static CompletableFuture<Response> observed(RequestObservation observation, CompletableFuture<Response> future) {
        if (future == null) {
            observation.failed(new IllegalStateException("No response"));
            observation.close();
            return null;
        }
        future.whenComplete((response, failure) -> {
            if (failure != null) {
                observation.failed(failure);
            } else {
                responded(observation, response);
            }
            observation.close();
        });
        return future;
    }

    private static void responded(RequestObservation observation, Response response) {
        if (response == null) {
            observation.responded(-1, true);
        } else {
            observation.responded(response.statusCode(), response.body() == null || response.body().length == 0);
        }
    }
}
