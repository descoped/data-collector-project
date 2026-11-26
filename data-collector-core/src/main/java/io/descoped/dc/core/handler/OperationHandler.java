package io.descoped.dc.core.handler;

import io.descoped.dc.api.ConfigurationMap;
import io.descoped.dc.api.CorrelationIds;
import io.descoped.dc.api.PageContext;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.error.ExecutionException;
import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.HttpStatusRetryWhile;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.api.node.Operation;
import io.descoped.dc.api.node.Validator;
import io.descoped.dc.api.security.Masking;
import io.descoped.dc.api.util.CommonUtils;
import io.descoped.dc.core.executor.Executor;
import io.descoped.dc.core.health.HealthWorkerMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The base of the handlers of HTTP operations: it sends the request with the context's and the node's headers to the
 * evaluated URL, retries on errors, validates the response, adds the page to the content store when the paginate
 * handler asks for it, and executes the node's steps on the response.
 *
 * @param <T> the type of operation node the handler executes
 */
public abstract class OperationHandler<T extends Operation> extends AbstractNodeHandler<T> {

    private static final String RETRY_COUNT = "data.collector.http.request.retryCount";

    private static final String NAP_ON_RETRY = "data.collector.http.request.napOnRetryInMillis";

    private static final Logger LOG = LoggerFactory.getLogger(OperationHandler.class);

    /**
     * Creates a handler for an operation node.
     *
     * @param node the node to execute
     */
    protected OperationHandler(T node) {
        super(node);
    }

    static void copyInputHeadersToRequestBuilder(ExecutionContext input, Request.Builder requestBuilder) {
        // todo fails here because there is a headers in both globalState and state, where state is read and contains an empty map
        Headers globalHeaders = input.state(Headers.class);
        if (globalHeaders != null) {
            globalHeaders.asMap().forEach((name, values) -> values.forEach(value -> requestBuilder.header(name, value)));
        }
    }

    static void copyNodeHeadersToRequestBuilder(Operation node, ExecutionContext context, Request.Builder requestBuilder) {
        if (node.headers() == null) {
            return;
        }

        ExpressionLanguage el = new ExpressionLanguage(context);
        for (Map.Entry<String, List<String>> headerEntry : node.headers().asMap().entrySet()) {
            String name = headerEntry.getKey();
            for (String value : headerEntry.getValue()) {
                if (el.isExpression(value)) {
                    String evaluatedValue = el.evaluateExpressions(value);
                    requestBuilder.header(name, evaluatedValue);
                } else {
                    requestBuilder.header(name, value);
                }
            }
        }
    }

    private String evaluatedUrl(ExecutionContext context) {
        ExpressionLanguage el = new ExpressionLanguage(context);
        return el.evaluateExpressions(node.url());
    }

    int beforeRequest(ExecutionContext input) {
        // prepare get request
        ConfigurationMap configurationMap = input.services().get(ConfigurationMap.class);
        return configurationMap != null ? Integer.parseInt(configurationMap.get("data.collector.http.request.timeout.seconds")) : 15;
    }

    Response doRequest(ExecutionContext input, int requestTimeout, Request.Builder requestBuilder) {
        // prepare request headers
        copyInputHeadersToRequestBuilder(input, requestBuilder);
        copyNodeHeadersToRequestBuilder(node, input, requestBuilder);

        // evaluate url with expressions
        String url = evaluatedUrl(input);
        requestBuilder.url(url);

        // Expose node.url() to ByteBuddy Agent
        input.state("PROMETHEUS_METRICS_REQUEST_URL", url);

        // execute http get
        Client client = input.services().get(Client.class);
        Request request = requestBuilder.build();
        long currentMillisSeconds = System.currentTimeMillis();

        // override default retry count
        ConfigurationMap configurationMap = input.services().get(ConfigurationMap.class);
        int retryCount = configurationMap.contains(RETRY_COUNT) ? Integer.parseInt(configurationMap.get(RETRY_COUNT)) : 3;
        int napOnRetryInMillis = configurationMap.contains(NAP_ON_RETRY) ? Integer.parseInt(configurationMap.get(NAP_ON_RETRY)) : 150;

        /*
         * Execute Request
         */
        Response response = sendAndRetryRequestOnError(input, client, request, requestTimeout, retryCount, napOnRetryInMillis);

        long futureMillisSeconds = System.currentTimeMillis();
        long durationMillisSeconds = futureMillisSeconds - currentMillisSeconds;

        HealthWorkerMonitor monitor = input.services().get(HealthWorkerMonitor.class);
        if (monitor != null) {
            monitor.request().incrementCompletedRequestCount();
            monitor.request().updateLastRequestDurationMillisSeconds(durationMillisSeconds);
            monitor.request().addRequestDurationMillisSeconds(durationMillisSeconds);
        }

        if (response == null) {
            throw new IllegalStateException("No response received. An unhandled error occurred: " + Masking.url(request.url()));
        }

        // prepare http-request-info used by content producer; the manifest keeps the names of secret headers, not their values
        Masking masking = Masking.of(configurationMap);
        HttpRequestInfo httpRequestInfo = new HttpRequestInfo(CorrelationIds.create(input), url, response.statusCode(),
                masking.headers(request.headers()), masking.headers(response.headers()), durationMillisSeconds);
        input.state(HttpRequestInfo.class, httpRequestInfo);

        // add page content
        boolean addPageContent = input.state(PaginateHandler.ADD_PAGE_CONTENT) != null && (Boolean) input.state(PaginateHandler.ADD_PAGE_CONTENT);

        // TODO end-of-stream is determined in Sequence. An unintended empty page will be added to content store
        if (addPageContent) {
            ContentStore contentStore = input.services().get(ContentStore.class);
            String topicName = node.configurations().flowContext().topic();
            if (topicName == null) {
                throw new IllegalStateException("Unable to resolve topic!");
            }
            // TODO consider to expand with EL and Query
            String positionVariable = input.state(PaginateHandler.ADD_PAGE_CONTENT_TO_POSITION);
            if (positionVariable == null || positionVariable.isBlank()) {
                throw new ExecutionException(String.format("The position is undefined for %s.addPageContent(positionVariable)!", node.id()));
            }
            Object position = input.variable(positionVariable);

            if (position == null) {
                throw new ExecutionException(String.format("Unable to resolve position for function %s.addPageContent(%s)!", node.id(), positionVariable));
            }
            contentStore.addPaginationDocument(topicName, position.toString(), "page", response.body(), httpRequestInfo);
            input.releaseState(PaginateHandler.ADD_PAGE_CONTENT);
        }
        return response;
    }

    ExecutionContext handleResponse(ExecutionContext input, Response response) {
        // create output
        ExecutionContext accumulated = ExecutionContext.empty();
        accumulated.state(Response.class, response);

        PageContext.Builder pageContextBuilder = new PageContext.Builder();
        pageContextBuilder.addNextPositionVariableNames(node.returnVariables());

        // handle step nodes and captures exceptions
        AtomicReference<Throwable> failureCause = new AtomicReference<>();
        for (Node step : node.steps()) {
            ExecutionContext stepInput = ExecutionContext.of(input).merge(accumulated);
            stepInput.state(PageContext.Builder.class, pageContextBuilder);
            // the body handler belongs to this operation: a request in a step reads its own response, and must not
            // take over (and restart) the file this operation's response is streamed from; the steps read that file
            // through the response
            if (stepInput.state(io.descoped.dc.api.http.BodyHandler.class) != null) {
                stepInput.releaseState(io.descoped.dc.api.http.BodyHandler.class);
            }

            try {
                ExecutionContext stepOutput = Executor.execute(step, stepInput);
                accumulated.merge(stepOutput);
            } catch (Exception e) {
                if (!failureCause.compareAndSet(null, e)) {
                    failureCause.get().addSuppressed(e);
                }
            }
        }

        // return only variables declared in returnVariables
        ExecutionContext output = ExecutionContext.of(accumulated); //.merge(correlationIdsAfterChildren.context());
        node.returnVariables().forEach(variableKey -> output.state(variableKey, accumulated.state(variableKey)));

        // if a step handler did not build a PageContext, we gather what we have an pass it on. This occurs when any of SequenceHandler, NextPageHandler or ParallelHandler was not executed
        PageContext pageContext = accumulated.state(PageContext.class);
        if (pageContext == null) {
            pageContext = pageContextBuilder.build();
        }
        output.state(PageContext.class, pageContext);

        // graceful exception handling
        Throwable stepNodeThrowable = failureCause.get();
        if (stepNodeThrowable instanceof EndOfStreamException) {
            output.state(PageContext.class).setEndOfStream(true);
        } else if (stepNodeThrowable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        } else if (stepNodeThrowable instanceof Error error) {
            throw error;
        } else if (stepNodeThrowable instanceof Exception) {
            throw new ExecutionException(stepNodeThrowable);
        }

        return output;
    }

    private Response sendAndRetryRequestOnError(ExecutionContext context, Client client, Request request, int requestTimeout, int retryCount, int napOnRetryInMillis) {
        Response response = null;
        for (int retry = 0; retry < retryCount; retry++) {
            try {
                // retryWhile handling
                if (context.state(HttpStatusRetryWhile.class) != null) {
                    boolean tryAgain = true;
                    while (tryAgain) {
                        // execute request
                        response = executeRequest(context, client, request, requestTimeout, true);

                        // execute RetryWhileHandler (match criteria and wait)
                        HttpStatusRetryWhile retryWhile = context.state(HttpStatusRetryWhile.class);
                        ExecutionContext retryWhileOutput = Executor.execute(retryWhile, ExecutionContext.empty().state(Request.class, request).state(Response.class, response));
                        tryAgain = retryWhileOutput.state(HttpStatusRetryWhileHandler.TRY_AGAIN);
                    }
                    validateResponse(context, request, response);

                } else {
                    // default handling
                    response = executeRequest(context, client, request, requestTimeout, false);
                }
                break;
            } catch (Exception e) {
                // an interrupted thread (the worker is being stopped) does not try again
                if (Thread.currentThread().isInterrupted()) {
                    throw e instanceof RuntimeException runtimeException ? runtimeException : new ExecutionException(e);
                }
                HealthWorkerMonitor monitor = context.services().get(HealthWorkerMonitor.class);
                if (monitor != null) {
                    monitor.request().incrementRequestRetryOnFailureCount();
                }
                LOG.error("Request error occurred - retrying {} of {}: {}\nCause: {}", retry + 1, retryCount, Masking.url(request.url()), CommonUtils.captureStackTrace(e));
                nap(napOnRetryInMillis);
            }
        }
        return response;
    }

    // propagate bodyHandler; a file handler receives one request's body, so a retry gets a new one of the same file
    private static io.descoped.dc.api.http.BodyHandler<?> bodyHandler(ExecutionContext context) {
        io.descoped.dc.api.http.BodyHandler<?> bodyHandler = context.state(io.descoped.dc.api.http.BodyHandler.class);
        if (bodyHandler instanceof FileBodyHandler fileBodyHandler && !fileBodyHandler.claim()) {
            FileBodyHandler next = fileBodyHandler.restart();
            next.claim();
            context.state(io.descoped.dc.api.http.BodyHandler.class, next);
            return next;
        }
        return bodyHandler;
    }

    private Response executeRequest(ExecutionContext context, Client client, Request request, int requestTimeout, boolean inRetryWhileState) {
        AtomicReference<Throwable> failureCause = new AtomicReference<>();

        io.descoped.dc.api.http.BodyHandler<?> bodyHandler = bodyHandler(context);

        CompletableFuture<Response> requestFuture = (bodyHandler == null ? client.sendAsync(request) : client.sendAsync(request, bodyHandler))
                .exceptionally(throwable -> {
                    failureCause.compareAndSet(null, throwable);

                    if (throwable instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    if (throwable instanceof Error error) {
                        throw error;
                    }
                    throw new ExecutionException(throwable);
                });

        try {
            Response response = requestFuture.get(requestTimeout, TimeUnit.SECONDS);

            if (!inRetryWhileState) validateResponse(context, request, response);

            // TODO should exception be rethrown
            if (failureCause.get() != null && LOG.isErrorEnabled()) {
                LOG.error("Captured completable exception: {}", CommonUtils.captureStackTrace(failureCause.get()));
            }

            return response;

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            failureCause.compareAndSet(null, e);

            if (failureCause.get() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (failureCause.get() instanceof Error error) {
                throw error;
            }
            throw new ExecutionException(failureCause.get());
        }
    }

    private void validateResponse(ExecutionContext context, Request request, Response response) {
        // fire validation handlers
        if (response != null) {
            for (Validator responseValidator : node.responseValidators()) {
                Executor.execute(responseValidator, ExecutionContext.of(context).state(Request.class, request).state(Response.class, response));
            }
        }
    }

    private void nap(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
