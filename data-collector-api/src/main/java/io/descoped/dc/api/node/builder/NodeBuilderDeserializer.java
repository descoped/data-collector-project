package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.descoped.dc.api.Specification;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * TODO Rewrite to a generic deserializer
 */
public class NodeBuilderDeserializer extends StdDeserializer<AbstractBuilder> {

    private static final long serialVersionUID = 1L;

    private static final String VARIABLES = "variables";
    private static final String SPLIT_QUERY = "splitQuery";
    private static final String PIPES = "pipes";
    private static final String QUERY = "query";
    private static final String EXPRESSION = "expression";
    private static final String RESPONSE_VALIDATORS = "responseValidators";
    private static final String RETURN_VARIABLES = "returnVariables";

    public NodeBuilderDeserializer() {
        this(null);
    }

    protected NodeBuilderDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public AbstractBuilder deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        return handleNodeBuilder(0, context, new LinkedList<>(), null, node);
    }

    private String nodePath(Deque<JsonNode> ancestors) {
        return "/" + ancestors.stream().filter(Objects::nonNull).map(node -> {
            String id = node.has("id") ? node.get("id").textValue() : null;
            String type = node.has("type") ? node.get("type").textValue() : null;
            return String.format("%s%s", type, id == null ? "" : String.format("[%s]", id));
        }).collect(Collectors.joining("/"));
    }

    private AbstractBuilder handleNodeBuilder(int depth, DeserializationContext context, Deque<JsonNode> ancestors,
                                              JsonNode previousNode, JsonNode currentNode) {
        if (currentNode == null || !currentNode.has("type")) {
            throw new IllegalArgumentException(String.format("Error resolving node: %s => %s", nodePath(ancestors), ancestors.peekLast()));
        }
        BuilderType type = BuilderType.parse(currentNode.get("type").textValue());

        ancestors.add(previousNode);
        AbstractBuilder nextNodeBuilder = buildNodeBuilder(depth, context, ancestors, currentNode, type);
        ancestors.remove(previousNode);
        return nextNodeBuilder;
    }

    private AbstractBuilder buildNodeBuilder(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode, BuilderType type) {
        Objects.requireNonNull(type);
        switch (type) {
            case Specification: {
                return buildSpecification(depth, context, ancestors, currentNode);
            }

            case SpecificationContext: {
                return buildSpecificationContext(currentNode);
            }

            case Security: {
                return buildSecurity(depth, context, ancestors, currentNode);
            }

            case JwtIdentity: {
                return buildJwtIdentity(currentNode);
            }

            case Paginate: {
                return buildPaginate(depth, context, ancestors, currentNode);
            }

            case Sequence: {
                return buildSequence(depth, context, ancestors, currentNode);
            }

            case NextPage: {
                return buildNextPage(depth, context, ancestors, currentNode);
            }

            case Parallel: {
                return buildParallel(depth, context, ancestors, currentNode);
            }

            case ForEach: {
                return buildForEach(depth, context, ancestors, currentNode);
            }

            case Execute: {
                return buildExecute(depth, context, ancestors, currentNode);
            }

            case Process: {
                return buildProcess(currentNode);
            }

            case Console: {
                return new ConsoleBuilder();
            }

            case QueryBody: {
                return new BodyBuilder();
            }

            case QueryEval: {
                return buildQueryEval(depth, context, ancestors, currentNode);
            }

            case QueryXPath: {
                return new XPathBuilder(currentNode.get(EXPRESSION).textValue());
            }

            case QueryJsonToken: {
                return new JsonTokenBuilder();
            }

            case QueryXmlToken: {
                return new XmlTokenBuilder();
            }

            case QueryJqPath: {
                return new JqPathBuilder(currentNode.get(EXPRESSION).textValue());
            }

            case QueryRegEx: {
                return new RegExBuilder((QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, currentNode.get(QUERY)), currentNode.get(EXPRESSION).textValue());
            }

            case ConditionWhenVariableIsNull: {
                WhenVariableIsNullBuilder builder = new WhenVariableIsNullBuilder();
                builder.identifier(currentNode.get("identifier").textValue());
                return builder;
            }

            case ConditionWhenExpressionIsTrue: {
                WhenExpressionIsTrueBuilder builder = new WhenExpressionIsTrueBuilder();
                builder.identifier(currentNode.get("identifier").textValue());
                return builder;
            }

            case AddContent: {
                return buildAddContent(currentNode);
            }

            case Publish: {
                return new PublishBuilder(currentNode.get("positionVariableExpression").textValue());
            }

            case Get: {
                return buildGet(depth, context, ancestors, currentNode);
            }

            case Post: {
                return buildPost(depth, context, ancestors, currentNode);
            }

            case Put: {
                return buildPut(depth, context, ancestors, currentNode);
            }

            case Delete: {
                return buildDelete(depth, context, ancestors, currentNode);
            }

            case BodyPublisher: {
                return buildBodyPublisher(depth, context, ancestors, currentNode);
            }

            case StringBodyPublisherProducer: {
                String data = currentNode.get("data").textValue();
                return new StringBodyPublisherProducerBuilder(data);
            }

            case JwtIdentityTokenBodyPublisherProducer: {
                return buildJwtIdentityTokenBodyPublisherProducer(currentNode);
            }

            case HttpStatusRetryWhile: {
                return buildHttpStatusRetryWhile(currentNode);
            }

            case HttpStatusValidation: {
                return buildHttpStatusValidation(depth, context, ancestors, currentNode);
            }

            case HttpResponseBodyContains: {
                QueryBuilder queryBuilder = (QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, currentNode.get("queryBuilder"));
                String equalToStringLiteral = currentNode.get("equalToStringLiteral").asText();
                return new BodyContainsBuilder(queryBuilder, equalToStringLiteral);
            }

            default: {
                throw new UnsupportedOperationException("NodeBuilder type '" + type + "' NOT supported!");
            }
        }

    }

    private AbstractBuilder buildSpecification(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        SpecificationBuilder builder = Specification.start(
                currentNode.get("id").textValue(),
                currentNode.get("name").textValue(),
                currentNode.get("startFunctionId").textValue()
        );

        JsonNode configureNode = currentNode.get("configure");
        if (configureNode != null) {
            for (JsonNode configurationNode : configureNode) {
                ConfigurationBuilder configurationBuilder = (ConfigurationBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, configurationNode);
                builder.configure(configurationBuilder);
            }
        }

        currentNode.get("functions").properties().forEach(entry ->
                builder.function((NodeWithIdBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, entry.getValue())));

        return builder;
    }

    private AbstractBuilder buildSpecificationContext(JsonNode currentNode) {
        SpecificationContextBuilder builder = new SpecificationContextBuilder();

        JsonNode headerNode = currentNode.get("headers");
        if (headerNode != null) {
            headerNode.properties().forEach(entry -> {
                entry.getValue().forEach(headerValueNode -> builder.header(entry.getKey(), headerValueNode.textValue()));
            });
        }

        JsonNode variableNode = currentNode.get(VARIABLES);
        if (variableNode != null) {
            variableNode.properties().forEach(entry -> {
                Object value;
                if (entry.getValue().isInt()) {
                    value = entry.getValue().intValue();
                } else if (entry.getValue().isBoolean()) {
                    value = entry.getValue().booleanValue();
                } else if (entry.getValue().isTextual()) {
                    value = entry.getValue().textValue();
                } else if (entry.getValue().isNull()) {
                    value = null;
                } else {
                    throw new IllegalArgumentException("The context variable type is not supported: " + entry.getKey());
                }
                builder.variable(entry.getKey(), value);
            });
        }

        JsonNode globalStateNode = currentNode.get("globalState");
        if (globalStateNode != null) {
            globalStateNode.properties().forEach(entry -> {
                builder.globalState(entry.getKey(), entry.getValue().textValue());
            });
        }

        return builder;
    }

    private AbstractBuilder buildSecurity(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        SecurityBuilder builder = new SecurityBuilder();

        JsonNode sslBundleNameNode = currentNode.get("bundleName");
        if (sslBundleNameNode != null) {
            builder.sslBundleName(sslBundleNameNode.textValue());
        }

        ArrayNode identityNodes = (ArrayNode) currentNode.get("identities");
        if (identityNodes != null) {
            identityNodes.forEach(identityNode -> {
                builder.identity((IdentityBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, identityNode));
            });
        }

        return builder;
    }

    private AbstractBuilder buildJwtIdentity(JsonNode currentNode) {
        String id = currentNode.get("id").textValue();

        JwtHeaderClaims headerClaims = new JwtHeaderClaims();
        JsonNode headerClaimsNode = currentNode.get("headerClaims");
        if (headerClaimsNode != null) {
            headerClaimsNode.properties().forEach(entry -> {
                String value = entry.getValue() == null ? null : entry.getValue().textValue(); // todo only support string values
                headerClaims.headerClaims.put(entry.getKey(), value);
            });
        }

        JwtClaims claims = new JwtClaims();
        JsonNode claimsNode = currentNode.get("claims");
        if (claimsNode != null) {
            claimsNode.properties().forEach(entry -> {
                String value = entry.getValue() == null ? null : entry.getValue().textValue(); // todo only support string values
                claims.claims.put(entry.getKey(), value);
            });
        }

        return new JwtIdentityBuilder(id, headerClaims, claims);
    }

    private AbstractBuilder buildPaginate(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        PaginateBuilder builder = new PaginateBuilder(currentNode.get("id").textValue());

        currentNode.get(VARIABLES).properties().forEach(entry -> builder.variable(entry.getKey(), entry.getValue().textValue()));

        if (currentNode.has("addPageContent") && currentNode.get("addPageContent").asBoolean()) {
            builder.addPageContent(currentNode.get("positionVariable").textValue());
        }

        currentNode.get("iterate").forEach(child -> builder.iterate((ExecuteBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, child)));

        JsonNode thresholdNode = currentNode.get("threshold");
        builder.prefetchThreshold(thresholdNode.asInt());

        JsonNode conditionBuilderNode = currentNode.get("until");
        builder.until((ConditionBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, conditionBuilderNode));

        return builder;
    }

    private AbstractBuilder buildSequence(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        SequenceBuilder builder = new SequenceBuilder(null);

        QueryBuilder splitBuilder = (QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, currentNode.get(SPLIT_QUERY));
        builder.splitBuilder(splitBuilder);

        QueryBuilder expectedBuilder = (QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, currentNode.get("expectedQuery"));
        builder.expected(expectedBuilder);

        return builder;
    }

    private AbstractBuilder buildNextPage(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        NextPageBuilder builder = new NextPageBuilder();

        currentNode.get("outputs").properties().forEach(entry ->
                builder.output(
                        entry.getKey(),
                        (QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, entry.getValue())
                )
        );

        return builder;
    }

    private AbstractBuilder buildParallel(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        QueryBuilder splitBuilder = (QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, currentNode.get(SPLIT_QUERY));
        ParallelBuilder builder = new ParallelBuilder(splitBuilder);

        currentNode.get(VARIABLES).properties().forEach(entry ->
                builder.variable(entry.getKey(), (QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, entry.getValue())));

        JsonNode stepsNode = currentNode.get(PIPES);
        if (stepsNode != null) {
            stepsNode.forEach(stepNode -> builder.pipe((NodeBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, stepNode)));
        }

        JsonNode publishJsonNode = currentNode.get("publish");
        if (publishJsonNode != null) {
            PublishBuilder publishBuilder = (PublishBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, publishJsonNode);
            builder.publish(publishBuilder);
        }

        return builder;
    }

    private AbstractBuilder buildForEach(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        JsonNode splitQueryNode = currentNode.get(SPLIT_QUERY);
        QueryBuilder splitQueryBuilder = (QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, splitQueryNode);
        ForEachBuilder builder = new ForEachBuilder(splitQueryBuilder);
        if (currentNode.has(PIPES)) {
            JsonNode pipeNodes = currentNode.get(PIPES);
            pipeNodes.forEach(pipeNode -> {
                NodeBuilder pipeBuilder = (NodeBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, pipeNode);
                builder.pipe(pipeBuilder);
            });
        }
        return builder;
    }

    private AbstractBuilder buildExecute(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        ExecuteBuilder builder = new ExecuteBuilder(currentNode.get("executeId").textValue());

        if (currentNode.has("requiredInputs")) {
            currentNode.get("requiredInputs").forEach(requiredInput -> builder.requiredInput(requiredInput.textValue()));
        }

        if (currentNode.has("inputVariables")) {
            currentNode.get("inputVariables").properties().forEach(entry ->
                    builder.inputVariable(entry.getKey(), (QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, entry.getValue())));
        }
        return builder;
    }

    // a specification names a ProcessorProvider, never a class: no class is loaded by a name from a specification
    private AbstractBuilder buildProcess(JsonNode currentNode) {
        if (currentNode.has("processorClass") || !currentNode.hasNonNull("processor")) {
            throw new IllegalArgumentException("A Process names its processor's provider in 'processor'; 'processorClass' is not read");
        }
        ProcessBuilder builder = new ProcessBuilder(currentNode.get("processor").textValue());
        if (currentNode.has("requiredOutputs")) {
            currentNode.get("requiredOutputs").forEach(output -> builder.output(output.textValue()));
        }
        return builder;
    }

    private AbstractBuilder buildQueryEval(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        QueryBuilder queryBuilder = currentNode.has(QUERY) ? (QueryBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, currentNode.get(QUERY)) : null;
        String bindToVariableNode = currentNode.has("bindToVariable") ? currentNode.get("bindToVariable").textValue() : null;
        String expression = currentNode.get(EXPRESSION).textValue();
        return new EvalBuilder(queryBuilder, bindToVariableNode, expression);
    }

    private AbstractBuilder buildAddContent(JsonNode currentNode) {
        AddContentBuilder builder = new AddContentBuilder(currentNode.get("positionVariableExpression").textValue(), currentNode.get("contentKey").textValue());
        JsonNode stateNode = currentNode.get("state");
        if (stateNode != null) {

            stateNode.properties().forEach(entry -> {
                JsonNode value = entry.getValue();
                Object stateValue;
                if (value.isInt()) {
                    stateValue = value.intValue();
                } else if (value.isBoolean()) {
                    stateValue = value.booleanValue();
                } else if (value.isTextual()) {
                    stateValue = value.textValue();
                } else {
                    throw new IllegalArgumentException("The content state type is not supported: " + entry.getKey());
                }
                builder.storeState(entry.getKey(), stateValue);
            });
        }
        return builder;
    }

    private AbstractBuilder buildGet(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        GetBuilder builder = new GetBuilder(currentNode.get("id").textValue());

        builder.url(currentNode.get("url").textValue());

        JsonNode requestHeaders = currentNode.get("requestHeaders");
        if (requestHeaders != null) {
            requestHeaders.fieldNames().forEachRemaining(headerName -> {
                ArrayNode values = (ArrayNode) requestHeaders.get(headerName);
                for (int i = 0; i < values.size(); i++) {
                    JsonNode value = values.get(i);
                    String headerValue;
                    if (value.isInt()) {
                        headerValue = String.valueOf(value.intValue());
                    } else if (value.isBoolean()) {
                        headerValue = Boolean.toString(value.booleanValue());
                    } else if (value.isTextual()) {
                        headerValue = value.textValue();
                    } else {
                        throw new IllegalArgumentException("The context variable type is not supported: " + headerName);
                    }
                    builder.header(headerName, headerValue);
                }
            });
        }

        JsonNode retryWhileNodeArray = currentNode.get("retryWhile");
        if (retryWhileNodeArray != null) {
            retryWhileNodeArray.forEach(retryWhileNode -> {
                LeafNodeBuilder retryWhileBuilder = (LeafNodeBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, retryWhileNode);
                builder.retryWhile(retryWhileBuilder);
            });
        }

        JsonNode validateResponseNode = currentNode.get(RESPONSE_VALIDATORS);
        if (validateResponseNode != null) {
            validateResponseNode.forEach(validatorNode -> {
                LeafNodeBuilder validatorBuilder = (LeafNodeBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, validatorNode);
                builder.validate(validatorBuilder);
            });
        }

        ArrayNode returnVariablesNode = (ArrayNode) currentNode.get(RETURN_VARIABLES);
        if (returnVariablesNode != null) {
            returnVariablesNode.forEach(node -> builder.returnVariables(node.textValue()));
        }

        JsonNode stepsNode = currentNode.get(PIPES);
        if (stepsNode != null) {
            stepsNode.forEach(stepNode -> builder.pipe((NodeBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, stepNode)));
        }

        return builder;
    }

    private AbstractBuilder buildPost(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        PostBuilder builder = new PostBuilder(currentNode.get("id").textValue());

        builder.url(currentNode.get("url").textValue());
        readBodyOperation(depth, context, ancestors, currentNode, builder::data, builder::validate, builder::returnVariables, builder::pipe);
        return builder;
    }

    private AbstractBuilder buildPut(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        PutBuilder builder = new PutBuilder(currentNode.get("id").textValue());

        builder.url(currentNode.get("url").textValue());
        readBodyOperation(depth, context, ancestors, currentNode, builder::data, builder::validate, builder::returnVariables, builder::pipe);
        return builder;
    }

    // the body publisher, validators, return variables and pipes of a post or a put
    private void readBodyOperation(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode,
                                   Consumer<BodyPublisherBuilder> data, Consumer<LeafNodeBuilder> validate,
                                   Consumer<String> returnVariable, Consumer<NodeBuilder> pipe) {
        JsonNode bodyPublisherNode = currentNode.get("bodyPublisher");
        if (bodyPublisherNode != null) {
            data.accept((BodyPublisherBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, bodyPublisherNode));
        }

        JsonNode validateResponseNode = currentNode.get(RESPONSE_VALIDATORS);
        if (validateResponseNode != null) {
            validateResponseNode.forEach(validatorNode -> validate.accept((LeafNodeBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, validatorNode)));
        }

        ArrayNode returnVariablesNode = (ArrayNode) currentNode.get(RETURN_VARIABLES);
        if (returnVariablesNode != null) {
            returnVariablesNode.forEach(node -> returnVariable.accept(node.textValue()));
        }

        JsonNode stepsNode = currentNode.get(PIPES);
        if (stepsNode != null) {
            stepsNode.forEach(stepNode -> pipe.accept((NodeBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, stepNode)));
        }
    }

    private AbstractBuilder buildDelete(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        DeleteBuilder builder = new DeleteBuilder(currentNode.get("id").textValue());

        builder.url(currentNode.get("url").textValue());

        JsonNode validateResponseNode = currentNode.get(RESPONSE_VALIDATORS);
        if (validateResponseNode != null) {
            validateResponseNode.forEach(validatorNode -> {
                LeafNodeBuilder validatorBuilder = (LeafNodeBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, validatorNode);
                builder.validate(validatorBuilder);
            });
        }

        ArrayNode returnVariablesNode = (ArrayNode) currentNode.get(RETURN_VARIABLES);
        if (returnVariablesNode != null) {
            returnVariablesNode.forEach(node -> builder.returnVariables(node.textValue()));
        }

        JsonNode stepsNode = currentNode.get(PIPES);
        if (stepsNode != null) {
            stepsNode.forEach(stepNode -> builder.pipe((NodeBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, stepNode)));
        }

        return builder;
    }

    private AbstractBuilder buildBodyPublisher(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        BodyPublisherBuilder builder = new BodyPublisherBuilder();

        JsonNode plainTextDataNode = currentNode.get("plainTextData");
        if (plainTextDataNode != null) {
            BodyPublisherProducerBuilder bodyPublisherProducerBuilder =
                    (BodyPublisherProducerBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, plainTextDataNode);
            builder.plainText(bodyPublisherProducerBuilder);
        }

        JsonNode urlEncodedDataNode = currentNode.get("urlEncodedData");
        if (urlEncodedDataNode != null) {
            BodyPublisherProducerBuilder bodyPublisherProducerBuilder =
                    (BodyPublisherProducerBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, urlEncodedDataNode);
            builder.urlEncoded(bodyPublisherProducerBuilder);
        }

        // TODO add MultiPartFormData
        JsonNode partsData = currentNode.get("partsData");
        if (partsData != null) {
            partsData.forEach(partNode -> readPart(builder, partNode));
        }

        return builder;
    }

    private void readPart(BodyPublisherBuilder builder, JsonNode partNode) {
        String name = partNode.get("name").textValue();
        String filename = Optional.ofNullable(partNode.get("filename")).map(JsonNode::textValue).orElse(null);
        Charset charset = Optional.ofNullable(partNode.get("charset")).map(node -> Charset.forName(node.textValue())).orElse(StandardCharsets.UTF_8); // TODO add bodypart charset methods

        JsonNode valueNode = partNode.get("value");
        if (filename == null) {
            builder.textPart(name, valueNode.textValue());
        } else if (valueNode.isTextual()) {
            builder.formPart(name, filename, valueNode.textValue());
        } else {
            try {
                byte[] bytes = valueNode.binaryValue();
                builder.formPart(name, filename, bytes);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private AbstractBuilder buildJwtIdentityTokenBodyPublisherProducer(JsonNode currentNode) {
        String identityId = currentNode.get("identityId").textValue();
        String bindTo = currentNode.get("bindTo").textValue();
        String token = currentNode.get("token").textValue();
        JwtIdentityTokenBodyPublisherProducerBuilder builder = new JwtIdentityTokenBodyPublisherProducerBuilder();
        builder.identityId(identityId);
        builder.bindTo(bindTo);
        builder.token(token);
        return builder;
    }

    private AbstractBuilder buildHttpStatusRetryWhile(JsonNode currentNode) {
        HttpStatusRetryWhileBuilder builder = new HttpStatusRetryWhileBuilder();

        Integer statusCode = Integer.parseInt(currentNode.get("statusCode").asText());
        TimeUnit duration = TimeUnit.valueOf(currentNode.get("duration").asText());
        int amount = Integer.parseInt(currentNode.get("amount").asText());

        builder.is(statusCode, duration, amount);
        return builder;
    }

    private AbstractBuilder buildHttpStatusValidation(int depth, DeserializationContext context, Deque<JsonNode> ancestors, JsonNode currentNode) {
        HttpStatusValidationBuilder builder = new HttpStatusValidationBuilder();

        JsonNode successNode = currentNode.get("success");
        // "success" maps each status code to the response predicates it must meet, an empty list for none: for
        // example 200 to none, and 404 to an HttpResponseBodyContains of a QueryJqPath ".kode" equal to "SP-002"
        for (Iterator<Map.Entry<String, JsonNode>> it = successNode.properties().iterator(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> elementNode = it.next();

            int statusCode = Integer.parseInt(elementNode.getKey());

            builder.success(statusCode);
            elementNode.getValue().forEach(responsePredicateNode -> {
                ResponsePredicateBuilder responsePredicateBuilder = (ResponsePredicateBuilder) handleNodeBuilder(depth + 1, context, ancestors, currentNode, responsePredicateNode);
                builder.success(statusCode, responsePredicateBuilder);
            });
        }

        JsonNode failedNode = currentNode.get("failed");
        if (failedNode != null) {
            failedNode.forEach(code -> builder.fail(code.intValue()));
        }

        return builder;
    }

}