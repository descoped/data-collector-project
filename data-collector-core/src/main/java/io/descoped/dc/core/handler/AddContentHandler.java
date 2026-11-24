package io.descoped.dc.core.handler;

import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.AddContent;

import java.util.LinkedHashMap;
import java.util.Map;

@Handler(forClass = AddContent.class)
public class AddContentHandler extends AbstractNodeHandler<AddContent> {

    public AddContentHandler(AddContent node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        ExpressionLanguage el = new ExpressionLanguage(context);
        String position = (String) el.evaluateExpression(node.positionVariableExpression());

        PageEntryState pageEntryState = context.state(PageEntryState.class);

        ContentStore contentStore = context.services().get(ContentStore.class);

        boolean bufferResponseBody = Boolean.TRUE.equals(context.state(ParallelHandler.ADD_BODY_CONTENT));

        // evaluate state - single expression for key and multiple expressions for value
        HttpRequestInfo httpRequestInfo = context.state(HttpRequestInfo.class);
        // Limitation: only save state for response-type: entry is supported
        Map<String, Object> evaluatedState = new LinkedHashMap<>();
        if (!bufferResponseBody && !node.state().isEmpty()) {
            evaluateState(el, evaluatedState);
        }

        String topicName = node.configurations().flowContext().topic();
        if (topicName == null) {
            throw new IllegalStateException("Unable to resolve topic!");
        }

        String contentKey = node.contentKey();
        if (el.isExpression(contentKey)) {
            contentKey = el.evaluateExpressions(contentKey);
        }

        byte[] content = bufferResponseBody ? context.state(Response.class).body() : pageEntryState.content;

        // feature request: decompression of 'content' can be handled here.

        if (bufferResponseBody) {
            contentStore.bufferDocument(topicName, position, contentKey, content, httpRequestInfo);
        } else {
            contentStore.bufferPaginationEntryDocument(topicName, position, contentKey, content, httpRequestInfo, evaluatedState);
        }

        return ExecutionContext.empty();
    }

    private void evaluateState(ExpressionLanguage el, Map<String, Object> evaluatedState) {
        for (Map.Entry<String, Object> entry : node.state().entrySet()) {
            String key = entry.getKey();

            if (el.isExpression(key)) {
                key = (String) el.evaluateExpression(key);
            }

            Object value = entry.getValue();

            if (value instanceof String text && el.isExpression(text)) {
                value = el.evaluateExpressions(text);
            }

            evaluatedState.put(key, value);
        }
    }
}
