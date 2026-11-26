package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.QueryResult;
import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.JsonToken;

import java.nio.file.Path;

/**
 * Executes a {@code JsonToken} query: the result is the file the response body was written to.
 */
@Handler(forClass = JsonToken.class)
public class JsonTokenHandler extends AbstractHandler<JsonToken> {

    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public JsonTokenHandler(JsonToken node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        Response response = context.state(Response.class);

        // todo is this used anywhere? maybe an empty EC should be returned
        BodyHandler<Path> bodyHandler = response.<Path>bodyHandler().orElseThrow();
        QueryResult<Path> queryResult = new QueryResult<>(bodyHandler.body());
        return ExecutionContext.empty().state(QueryResult.class, queryResult);
    }


}
