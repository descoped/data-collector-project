package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.Get;
import io.descoped.dc.api.node.HttpStatusRetryWhile;
import io.descoped.dc.api.node.JsonToken;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.api.node.Query;
import io.descoped.dc.api.node.Sequence;
import io.descoped.dc.api.node.XmlToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Executes a {@code Get} node: sends an HTTP GET request and handles its response. When a step of the node is a
 * sequence that splits with a JSON or XML token query, the body is written to a temporary file; the first
 * retry-while rule of the node, if any, applies to the request.
 */
@SuppressWarnings("unchecked")
@Handler(forClass = Get.class)
public class GetHandler extends OperationHandler<Get> {

    private static final Logger LOG = LoggerFactory.getLogger(GetHandler.class);

    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public GetHandler(Get node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        super.execute(input);
        int requestTimeout = beforeRequest(input);

        // detect Sequence jsonToken-query and mark OperationHandler to use TempFileBodyHandler
        // OperationHandler.executeRequest will resolve BodyHandler form input state context
        Optional<Sequence> sequenceNode = Optional.ofNullable(findSequenceNode());
        if (sequenceNode.isPresent()) {
            final Query bodyHandlerQuery = sequenceNode.get().splitToListQuery();
            if (bodyHandlerQuery instanceof JsonToken || bodyHandlerQuery instanceof XmlToken) {
                final TempFileBodyHandler bodyHandler = TempFileBodyHandler.ofFile();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Use BodyHandler: {} => {}", node.id(), bodyHandler.getClass().getSimpleName());
                }
                input.state(io.descoped.dc.api.http.BodyHandler.class, bodyHandler);
            }
        }

        Request.Builder requestBuilder = Request.newRequestBuilder().GET();

        // TODO needs to be fixed. RetryWhile is limited to one rule only.
        if (!node.retryWhile().isEmpty()) {
            input.state(HttpStatusRetryWhile.class, node.retryWhile().get(0));
        }

        Response response = doRequest(input, requestTimeout, requestBuilder);

        return handleResponse(input, response);
    }

    private Sequence findSequenceNode() {
        for (Node node : node.steps()) {
            if (node instanceof Sequence sequence) {
                return sequence;
            }
        }
        return null;
    }

}