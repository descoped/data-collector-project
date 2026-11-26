package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.Delete;

/**
 * Executes a {@code Delete} node: sends an HTTP DELETE request and handles its response.
 */
@SuppressWarnings("unchecked")
@Handler(forClass = Delete.class)
public class DeleteHandler extends OperationHandler<Delete> {

    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public DeleteHandler(Delete node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        super.execute(input);
        int requestTimeout = beforeRequest(input);

        Request.Builder requestBuilder = Request.newRequestBuilder().DELETE();

        Response response = doRequest(input, requestTimeout, requestBuilder);

        return handleResponse(input, response);
    }

}