package io.descoped.dc.core.handler;

import io.descoped.dc.api.ConfigurationMap;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.HttpStatus;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.HttpStatusValidation;
import io.descoped.dc.api.node.ResponsePredicate;
import io.descoped.dc.api.security.Masking;
import io.descoped.dc.core.executor.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Executes an {@code HttpStatusValidation} node: the response's status code is a success when it is in the node's
 * success list and has no response predicates or one of them holds; any other status code throws
 * {@code HttpErrorException}, with secret headers masked.
 */
@Handler(forClass = HttpStatusValidation.class)
public class HttpStatusValidationHandler extends AbstractHandler<HttpStatusValidation> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpStatusValidationHandler.class);

    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public HttpStatusValidationHandler(HttpStatusValidation node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        Request request = context.state(Request.class);
        Response response = context.state(Response.class);
        int statusCode = response.statusCode();

        // a status in the success list is a success when it has no predicates, or when one of them holds: a 404 is a
        // success when its body names one of the allowed errors
        boolean success = false;
        for (Map.Entry<HttpStatus, List<ResponsePredicate>> entry : node.success().entrySet()) {
            if (entry.getKey().code() == statusCode) {
                success = entry.getValue().isEmpty() || anyPredicateHolds(context, entry.getValue());
                break;
            }
        }

        // TODO remove failure. Either we got a success, or it is a failure.
        if (!success) {
            // todo make explicit handling of 3xx redirect, 4xx client error, 5xx server error.
            boolean expectedErrorCodes = node.failed().stream().anyMatch(code -> statusCode == code.code());
            Masking masking = Masking.of(context.services().get(ConfigurationMap.class));
            if (expectedErrorCodes) {
                throwHttpErrorException(request, response, statusCode, masking);
            }
            throwHttpErrorException(request, response, statusCode, masking);
        }

        return ExecutionContext.empty();
    }

    // the predicates are evaluated until one holds
    private boolean anyPredicateHolds(ExecutionContext context, List<ResponsePredicate> responsePredicates) {
        for (ResponsePredicate responsePredicate : responsePredicates) {
            // response predicate handler must evaluate state(Response.class).body
            ExecutionContext output = Executor.execute(responsePredicate, ExecutionContext.of(context));
            if (Boolean.TRUE.equals(output.state(ResponsePredicate.RESPONSE_PREDICATE_RESULT))) {
                return true;
            }
        }
        return false;
    }

    // the message reaches logs and /health: secret headers are masked, and the body is given by length and type only
    void throwHttpErrorException(Request request, Response response, int statusCode, Masking masking) {
        HttpStatus failedStatus = HttpStatus.valueOf(statusCode);
        String requestHeaders = masking.headers(request.headers().asMap()).entrySet().stream().map(entry -> entry.getKey() + ": " +
                        String.join("\n\t", entry.getValue()))
                .collect(Collectors.joining("\n\t"));
        String responseHeaders = masking.headers(response.headers().asMap()).entrySet().stream().map(entry -> entry.getKey() + ": " +
                        String.join("\n\t", entry.getValue()))
                .collect(Collectors.joining("\n\t"));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Body of the response with status {} from {}:\n{}", failedStatus.code(), Masking.url(response.url()),
                    new String(response.body(), StandardCharsets.UTF_8));
        }
        throw new HttpErrorException(String.format("Error dealing with response: %s [%s]\u00A0%s%nBody: %s%nRequestHeaders:[%n\t%s%n]%nResponseHeaders:[%n\t%s%n]",
                Masking.url(response.url()), failedStatus.code(), failedStatus.reason(), Masking.body(response.body(), response.headers()),
                requestHeaders, responseHeaders));

    }

}
