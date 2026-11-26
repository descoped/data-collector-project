package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.HttpStatus;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.HttpStatusValidation;
import io.descoped.dc.api.node.ResponsePredicate;
import io.descoped.dc.core.executor.Executor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Handler(forClass = HttpStatusValidation.class)
public class HttpStatusValidationHandler extends AbstractHandler<HttpStatusValidation> {

    public HttpStatusValidationHandler(HttpStatusValidation node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        Request request = context.state(Request.class);
        Response response = context.state(Response.class);
        int statusCode = response.statusCode();

        boolean success = false;
        for (Map.Entry<HttpStatus, List<ResponsePredicate>> entry : node.success().entrySet()) {
            // true when statusCode is in the success list: its predicates are evaluated, but do not decide
            if (entry.getKey().code() == statusCode) {
                evaluatePredicates(context, entry.getValue());
                success = true;
                break;
            }
        }

        // TODO remove failure. Either we got a success, or it is a failure.
        if (!success) {
            // todo make explicit handling of 3xx redirect, 4xx client error, 5xx server error.
            boolean expectedErrorCodes = node.failed().stream().anyMatch(code -> statusCode == code.code());
            if (expectedErrorCodes) {
                throwHttpErrorException(request, response, statusCode);
            }
            throwHttpErrorException(request, response, statusCode);
        }

        return ExecutionContext.empty();
    }

    // the predicates are evaluated until one is false
    private void evaluatePredicates(ExecutionContext context, List<ResponsePredicate> responsePredicates) {
        for (ResponsePredicate responsePredicate : responsePredicates) {
            // response predicate handler must evaluate state(Response.class).body
            ExecutionContext output = Executor.execute(responsePredicate, ExecutionContext.of(context));
            boolean test = output.state(ResponsePredicate.RESPONSE_PREDICATE_RESULT);
            if (!test) {
                break;
            }
        }
    }

    void throwHttpErrorException(Request request, Response response, int statusCode) {
        HttpStatus failedStatus = HttpStatus.valueOf(statusCode);
        String requestHeaders = request.headers().asMap().entrySet().stream().map(entry -> entry.getKey() + ": " +
                        String.join("\n\t", entry.getValue()))
                .collect(Collectors.joining("\n\t"));
        String responseHeaders = response.headers().asMap().entrySet().stream().map(entry -> entry.getKey() + ": " +
                        String.join("\n\t", entry.getValue()))
                .collect(Collectors.joining("\n\t"));
        throw new HttpErrorException(String.format("Error dealing with response: %s [%s]\u00A0%s%nBody:%n\t%s%nRequestHeaders:[%n\t%s%n]%nResponseHeaders:[%n\t%s%n]",
                response.url(), failedStatus.code(), failedStatus.reason(), new String(response.body(), StandardCharsets.UTF_8), requestHeaders, responseHeaders));

    }

}
