package io.descoped.dc.api.node.builder;

/**
 * The types of builder. A builder serializes its type as the {@code type} property, and
 * {@link NodeBuilderDeserializer} reads that property to choose the builder to create.
 */
public enum BuilderType {
    /** A specification, {@link SpecificationBuilder}. */
    Specification(SpecificationBuilder.class),
    /** A specification's context: headers, variables and global configuration, {@link SpecificationContextBuilder}. */
    SpecificationContext(SpecificationContextBuilder.class),
    /** The security configuration, {@link SecurityBuilder}. */
    Security(SecurityBuilder.class),
    /** A JWT identity, {@link JwtIdentityBuilder}. */
    JwtIdentity(JwtIdentityBuilder.class),
    /** A request body made from a JWT identity token, {@link JwtIdentityTokenBodyPublisherProducerBuilder}. */
    JwtIdentityTokenBodyPublisherProducer(JwtIdentityTokenBodyPublisherProducerBuilder.class),
    /** A paginate node, {@link PaginateBuilder}. */
    Paginate(PaginateBuilder.class),
    /** A for-each node, {@link ForEachBuilder}. */
    ForEach(ForEachBuilder.class),
    /** A sequence node, {@link SequenceBuilder}. */
    Sequence(SequenceBuilder.class),
    /** A next-page node, {@link NextPageBuilder}. */
    NextPage(NextPageBuilder.class),
    /** A parallel node, {@link ParallelBuilder}. */
    Parallel(ParallelBuilder.class),
    /** An execute node, {@link ExecuteBuilder}. */
    Execute(ExecuteBuilder.class),
    /** A process node, {@link ProcessBuilder}. */
    Process(ProcessBuilder.class),
    /** A query that returns the response body, {@link BodyBuilder}. */
    QueryBody(BodyBuilder.class),
    /** An expression language query, {@link EvalBuilder}. */
    QueryEval(EvalBuilder.class),
    /** An XPath query, {@link XPathBuilder}. */
    QueryXPath(XPathBuilder.class),
    /** A JSON token query, {@link JsonTokenBuilder}. */
    QueryJsonToken(JsonTokenBuilder.class),
    /** An XML token query, {@link XmlTokenBuilder}. */
    QueryXmlToken(XmlTokenBuilder.class),
    /** A jq query, {@link JqPathBuilder}. */
    QueryJqPath(JqPathBuilder.class),
    /** A regular expression query, {@link RegExBuilder}. */
    QueryRegEx(RegExBuilder.class),
    /** A condition that holds when a variable is null, {@link WhenVariableIsNullBuilder}. */
    ConditionWhenVariableIsNull(WhenVariableIsNullBuilder.class),
    /** A condition that holds when an expression is true, {@link WhenExpressionIsTrueBuilder}. */
    ConditionWhenExpressionIsTrue(WhenExpressionIsTrueBuilder.class),
    /** An add-content node, {@link AddContentBuilder}. */
    AddContent(AddContentBuilder.class),
    /** A publish node, {@link PublishBuilder}. */
    Publish(PublishBuilder.class),
    /** An HTTP GET operation, {@link GetBuilder}. */
    Get(GetBuilder.class),
    /** An HTTP POST operation, {@link PostBuilder}. */
    Post(PostBuilder.class),
    /** An HTTP PUT operation, {@link PutBuilder}. */
    Put(PutBuilder.class),
    /** An HTTP DELETE operation, {@link DeleteBuilder}. */
    Delete(DeleteBuilder.class),
    /** A retry on HTTP status codes, {@link HttpStatusRetryWhileBuilder}. */
    HttpStatusRetryWhile(HttpStatusRetryWhileBuilder.class),
    /** A validation of the HTTP status code, {@link HttpStatusValidationBuilder}. */
    HttpStatusValidation(HttpStatusValidationBuilder.class),
    /** A request body, {@link BodyPublisherBuilder}. */
    BodyPublisher(BodyPublisherBuilder.class),
    /** A request body made from a string, {@link StringBodyPublisherProducerBuilder}. */
    StringBodyPublisherProducer(StringBodyPublisherProducerBuilder.class),
    /** A predicate on the response body, {@link BodyContainsBuilder}. */
    HttpResponseBodyContains(BodyContainsBuilder.class),
    /** A console node, {@link ConsoleBuilder}. */
    Console(ConsoleBuilder.class);

    /**
     * The builder class of this type.
     */
    public final Class<? extends AbstractBuilder> builderClass;

    BuilderType(Class<? extends AbstractBuilder> builderClass) {
        this.builderClass = builderClass;
    }

    /**
     * Finds the type with the given name, ignoring case.
     *
     * @param name the name of the type
     * @return the type, or {@code null} if no type has that name
     */
    public static BuilderType parse(String name) {
        for (BuilderType builderType : values()) {
            if (builderType.name().equalsIgnoreCase(name)) {
                return builderType;
            }
        }
        return null;
    }
}
