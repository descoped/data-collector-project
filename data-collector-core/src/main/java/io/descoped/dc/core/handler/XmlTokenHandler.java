package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.QueryResult;
import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.XmlToken;

import java.nio.file.Path;

/**
 * Executes an {@code XmlToken} query: the result is the file the response body was written to.
 */
@Handler(forClass = XmlToken.class)
public class XmlTokenHandler extends AbstractHandler<XmlToken> {

    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public XmlTokenHandler(XmlToken node) {
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
