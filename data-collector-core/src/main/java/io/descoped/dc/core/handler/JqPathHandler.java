package io.descoped.dc.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.node.JqPath;
import io.descoped.dc.api.util.CommonUtils;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Optional.ofNullable;

@Handler(forClass = JqPath.class)
public class JqPathHandler extends AbstractQueryHandler<JqPath> {

    private static final Logger LOG = LoggerFactory.getLogger(JqPathHandler.class);

    private static final Version JQ_VERSION = Versions.JQ_1_6;
    private final DocumentParserFeature jsonParser;
    private static final Scope rootScope = Scope.newEmptyScope();

    static {
        BuiltinFunctionLoader.getInstance().loadFunctions(JQ_VERSION, rootScope);
    }

    public JqPathHandler(JqPath node) {
        super(node);
        jsonParser = Queries.parserFor(node.getClass());
    }

    ObjectNode asDocument(Object data) {
        if (data instanceof ObjectNode objectNode) {
            return objectNode;

        } else if (data instanceof byte[] bytes) {
            return (ObjectNode) jsonParser.deserialize(bytes);

        } else if (data instanceof String text) {
            return (ObjectNode) jsonParser.deserialize(text.getBytes());

        } else {
            throw new IllegalArgumentException(String.format("Param value not supported [%s]: %s", data.getClass().getSimpleName(), data));
        }
    }

    @Override
    public List<?> evaluateList(Object data) {
        try {
            ObjectNode jsonNode = asDocument(data);
            if (jsonNode.isEmpty()) {
                return Collections.emptyList();
            }
            Scope childScope = Scope.newChildScope(rootScope);
            JsonQuery query = JsonQuery.compile(evaluateExpression(node.expression()), JQ_VERSION);
            List<JsonNode> result = new ArrayList<>();
            query.apply(childScope, jsonNode, result::add);
            return result;
        } catch (JsonQueryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object evaluateObject(Object data) {
        try {
            ObjectNode jsonNode = asDocument(data);
            Scope childScope = Scope.newChildScope(rootScope);
            JsonQuery query = JsonQuery.compile(evaluateExpression(node.expression()), JQ_VERSION);
            AtomicReference<JsonNode> outputRef = new AtomicReference<>();
            query.apply(childScope, jsonNode, outputRef::set);
            return outputRef.get();
        } catch (JsonQueryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String evaluateStringLiteral(Object data) {
        JsonNode jsonNode = null;
        try {
            jsonNode = (data instanceof JsonNode dataNode) ? dataNode : asDocument(data);
            Scope childScope = Scope.newChildScope(rootScope);
            JsonQuery query = JsonQuery.compile(evaluateExpression(node.expression()), JQ_VERSION);
            List<JsonNode> result = new ArrayList<>();
            query.apply(childScope, jsonNode, result::add);
            if (!result.isEmpty()) {
                JsonNode firstNode = result.get(0);
                switch (firstNode.getNodeType()) {
                    case NUMBER, STRING:
                        return firstNode.asText();

                    case NULL:
                        return null;

                    case ARRAY, BINARY, BOOLEAN, MISSING, OBJECT, POJO:
                    default: {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("Expression: '{}' caused failure. Node-type {} is not valid for string literal => {}", node.expression(), firstNode.getNodeType(), firstNode);
                        }
                        return null;
                    }
                }
            }
            return null;

        } catch (JsonQueryException e) {
            throw new IllegalStateException(e.getMessage() + " => Error with query: \"" + node.expression() + "\" " +
                    "on data:\n" +
                    ofNullable(jsonNode).map(JsonNode::toPrettyString).orElse(null) + "\n" +
                    CommonUtils.captureStackTrace(e));
        }
    }
}
