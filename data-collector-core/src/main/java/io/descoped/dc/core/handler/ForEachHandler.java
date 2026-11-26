package io.descoped.dc.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.QueryFeature;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.ForEach;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.core.executor.Executor;

import java.util.List;

/**
 * Executes a {@code ForEach} node: splits the current page entry's content (or the response body) into a list with the
 * node's split query, and executes the node's steps for each item, with the item as the page entry and the
 * {@link #ADD_BODY_CONTENT} state set to true.
 */
@Handler(forClass = ForEach.class)
public class ForEachHandler extends AbstractNodeHandler<ForEach> {

    /**
     * The name of the state that tells {@code AddContentHandler} to buffer the response body.
     */
    public static final String ADD_BODY_CONTENT = ParallelHandler.ADD_BODY_CONTENT;
    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public ForEachHandler(ForEach node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        super.execute(input);

        // get current response body
        Response response = input.state(Response.class);
        PageEntryState pageEntryState = input.state(PageEntryState.class);
        byte[] body = pageEntryState == null ? response.body() : pageEntryState.content;

        // evaluate queries for list and item
        DocumentParserFeature parser = Queries.parserFor(node.splitToListQuery().getClass());
        QueryFeature splitQuery = Queries.from(node.splitToListQuery());
        @SuppressWarnings("unchecked") // evaluateList returns the nodes of a JSON document
        List<JsonNode> list = (List<JsonNode>) splitQuery.evaluateList(body);

        for (JsonNode item : list) {
            byte[] serializedItem = parser.serialize(item);
            PageEntryState nestedEntry = new PageEntryState(item, serializedItem);
            for (Node step : node.steps()) {
                ExecutionContext stepInput = ExecutionContext.of(input).state(PageEntryState.class, nestedEntry).state(ADD_BODY_CONTENT, true);
                Executor.execute(step, stepInput);
            }
        }

        return ExecutionContext.empty();
    }
}
