package io.descoped.dc.api;

import io.descoped.dc.api.node.builder.AddContentBuilder;
import io.descoped.dc.api.node.builder.BodyBuilder;
import io.descoped.dc.api.node.builder.BodyContainsBuilder;
import io.descoped.dc.api.node.builder.BodyPublisherBuilder;
import io.descoped.dc.api.node.builder.ConsoleBuilder;
import io.descoped.dc.api.node.builder.DeleteBuilder;
import io.descoped.dc.api.node.builder.EvalBuilder;
import io.descoped.dc.api.node.builder.ExecuteBuilder;
import io.descoped.dc.api.node.builder.ForEachBuilder;
import io.descoped.dc.api.node.builder.GetBuilder;
import io.descoped.dc.api.node.builder.HttpStatusRetryWhileBuilder;
import io.descoped.dc.api.node.builder.HttpStatusValidationBuilder;
import io.descoped.dc.api.node.builder.JqPathBuilder;
import io.descoped.dc.api.node.builder.JsonTokenBuilder;
import io.descoped.dc.api.node.builder.JwtClaims;
import io.descoped.dc.api.node.builder.JwtHeaderClaims;
import io.descoped.dc.api.node.builder.JwtIdentityBuilder;
import io.descoped.dc.api.node.builder.JwtIdentityTokenBodyPublisherProducerBuilder;
import io.descoped.dc.api.node.builder.NextPageBuilder;
import io.descoped.dc.api.node.builder.PaginateBuilder;
import io.descoped.dc.api.node.builder.ParallelBuilder;
import io.descoped.dc.api.node.builder.PostBuilder;
import io.descoped.dc.api.node.builder.ProcessBuilder;
import io.descoped.dc.api.node.builder.PublishBuilder;
import io.descoped.dc.api.node.builder.PutBuilder;
import io.descoped.dc.api.node.builder.QueryBuilder;
import io.descoped.dc.api.node.builder.RegExBuilder;
import io.descoped.dc.api.node.builder.SecurityBuilder;
import io.descoped.dc.api.node.builder.SequenceBuilder;
import io.descoped.dc.api.node.builder.SpecificationContextBuilder;
import io.descoped.dc.api.node.builder.WhenExpressionIsTrueBuilder;
import io.descoped.dc.api.node.builder.WhenVariableIsNullBuilder;
import io.descoped.dc.api.node.builder.XPathBuilder;

/**
 * The entry point of the Java DSL: a factory of every specification builder.
 */
public class Builders {

    private Builders() {
    }

    /**
     * A builder of the specification context: topic, request headers, variables and global state.
     *
     * @return the builder
     */
    public static SpecificationContextBuilder context() {
        return new SpecificationContextBuilder();
    }

    /**
     * A builder of the security configuration: SSL bundle and identities.
     *
     * @return the builder
     */
    public static SecurityBuilder security() {
        return new SecurityBuilder();
    }

    /**
     * A builder of a JWT identity.
     *
     * @param id           the identity id
     * @param headerClaims the claims of the JWT header
     * @param claims       the claims of the JWT body
     * @return the builder
     */
    public static JwtIdentityBuilder jwt(String id, JwtHeaderClaims headerClaims, JwtClaims claims) {
        return new JwtIdentityBuilder(id, headerClaims, claims);
    }

    /**
     * A builder of a request body made from a JWT that an identity signs.
     *
     * @return the builder
     */
    public static JwtIdentityTokenBodyPublisherProducerBuilder jwtToken() {
        return new JwtIdentityTokenBodyPublisherProducerBuilder();
    }

    /**
     * Empty JWT header claims.
     *
     * @return the header claims
     */
    public static JwtHeaderClaims headerClaims() {
        return new JwtHeaderClaims();
    }

    /**
     * Empty JWT claims.
     *
     * @return the claims
     */
    public static JwtClaims claims() {
        return new JwtClaims();
    }

    /**
     * A builder of an HTTP GET operation.
     *
     * @param functionId the node id
     * @return the builder
     */
    public static GetBuilder get(String functionId) {
        return new GetBuilder(functionId);
    }

    /**
     * A builder of an HTTP POST operation.
     *
     * @param functionId the node id
     * @return the builder
     */
    public static PostBuilder post(String functionId) {
        return new PostBuilder(functionId);
    }

    /**
     * A builder of an HTTP PUT operation.
     *
     * @param functionId the node id
     * @return the builder
     */
    public static PutBuilder put(String functionId) {
        return new PutBuilder(functionId);
    }

    /**
     * A builder of an HTTP DELETE operation.
     *
     * @param functionId the node id
     * @return the builder
     */
    public static DeleteBuilder delete(String functionId) {
        return new DeleteBuilder(functionId);
    }

    /**
     * A builder of a rule that retries a request while the response has a status code.
     *
     * @return the builder
     */
    public static HttpStatusRetryWhileBuilder statusCode() {
        return new HttpStatusRetryWhileBuilder();
    }

    /**
     * A builder of a validator of the response status code.
     *
     * @return the builder
     */
    public static HttpStatusValidationBuilder status() {
        return new HttpStatusValidationBuilder();
    }

    /**
     * A builder of a request body.
     *
     * @return the builder
     */
    public static BodyPublisherBuilder bodyPublisher() {
        return new BodyPublisherBuilder();
    }

    /**
     * A builder of a response predicate that holds when a query on the body equals a string.
     *
     * @param queryBuilder         the query on the body
     * @param equalToStringLiteral the string the result must equal
     * @return the builder
     */
    public static BodyContainsBuilder bodyContains(QueryBuilder queryBuilder, String equalToStringLiteral) {
        return new BodyContainsBuilder(queryBuilder, equalToStringLiteral);
    }

    /**
     * A builder of a pagination.
     *
     * @param function the node id
     * @return the builder
     */
    public static PaginateBuilder paginate(String function) {
        return new PaginateBuilder(function);
    }

    /**
     * A sequence determines expected-positions from remote api
     *
     * @param builder the query that splits the response into a list
     * @return the builder
     */
    public static SequenceBuilder sequence(QueryBuilder builder) {
        return new SequenceBuilder(builder);
    }

    /**
     * A builder of the node that gives the next page's variables.
     *
     * @return the builder
     */
    public static NextPageBuilder nextPage() {
        return new NextPageBuilder();
    }

    /**
     * A builder of a node that runs its steps for each item of a list in parallel.
     *
     * @param builder the query that splits the response into the list
     * @return the builder
     */
    public static ParallelBuilder parallel(QueryBuilder builder) {
        return new ParallelBuilder(builder);
    }

    /**
     * A builder of a node that runs its steps for each item of a list.
     *
     * @param builder the query that splits the response into the list
     * @return the builder
     */
    public static ForEachBuilder forEach(QueryBuilder builder) {
        return new ForEachBuilder(builder);
    }

    /**
     * A builder of a node that executes another node by its id.
     *
     * @param function the id of the node to execute
     * @return the builder
     */
    public static ExecuteBuilder execute(String function) {
        return new ExecuteBuilder(function);
    }

    /**
     * A processor given by its class, for specifications built in Java. Written as JSON, the specification names the
     * class's {@link ProcessorProvider}, so it is read back as {@link #process(String)}: the two are not equal.
     *
     * @param processorClass the processor's class
     * @return the builder
     */
    public static ProcessBuilder process(Class<? extends Processor> processorClass) {
        return new ProcessBuilder(processorClass);
    }

    /**
     * A processor registered under a name by a {@link ProcessorProvider}.
     *
     * @param processor the provider's name
     * @return the builder
     */
    public static ProcessBuilder process(String processor) {
        return new ProcessBuilder(processor);
    }

    /**
     * A builder of a node that adds the entry's content to the content stream buffer.
     *
     * @param positionVariable the expression that gives the position
     * @param contentKey       the key the content is stored under
     * @return the builder
     */
    public static AddContentBuilder addContent(String positionVariable, String contentKey) {
        return new AddContentBuilder(positionVariable, contentKey);
    }

    /**
     * A builder of a node that publishes the buffered content of completed positions.
     *
     * @param positionVariable the expression that gives the position
     * @return the builder
     */
    public static PublishBuilder publish(String positionVariable) {
        return new PublishBuilder(positionVariable);
    }

    /**
     * A builder of a query that returns the response body unaltered.
     *
     * @return the builder
     */
    public static BodyBuilder body() {
        return new BodyBuilder();
    }

    /**
     * A builder of a query that evaluates an expression.
     *
     * @param elExpression the expression
     * @return the builder
     */
    public static EvalBuilder eval(String elExpression) {
        return new EvalBuilder(elExpression);
    }

    /**
     * A builder of a query that binds a sub-query's result to a variable and evaluates an expression.
     *
     * @param queryBuilder the sub-query
     * @param bindTo       the variable the sub-query's result is bound to
     * @param elExpression the expression
     * @return the builder
     */
    public static EvalBuilder eval(QueryBuilder queryBuilder, String bindTo, String elExpression) {
        return new EvalBuilder(queryBuilder, bindTo, elExpression);
    }

    /**
     * A builder of an XPath query.
     *
     * @param expression the XPath expression
     * @return the builder
     */
    public static XPathBuilder xpath(String expression) {
        return new XPathBuilder(expression);
    }

    /**
     * A builder of a query that splits a JSON body into entries by streaming its tokens.
     *
     * @return the builder
     */
    public static JsonTokenBuilder jsonToken() {
        return new JsonTokenBuilder();
    }

    /**
     * A builder of a jq query.
     *
     * @param expression the jq expression
     * @return the builder
     */
    public static JqPathBuilder jqpath(String expression) {
        return new JqPathBuilder(expression);
    }

    /**
     * A builder of a regular expression applied to a sub-query's result.
     *
     * @param queryBuilder the sub-query
     * @param expression   the regular expression
     * @return the builder
     */
    public static RegExBuilder regex(QueryBuilder queryBuilder, String expression) {
        return new RegExBuilder(queryBuilder, expression);
    }

    /**
     * A builder of a condition that holds when a variable is absent or {@code null}.
     *
     * @param identifier the variable name
     * @return the builder
     */
    public static WhenVariableIsNullBuilder whenVariableIsNull(String identifier) {
        return new WhenVariableIsNullBuilder().identifier(identifier);
    }

    /**
     * A builder of a condition that holds when an expression evaluates to {@code true}.
     *
     * @param expression the expression
     * @return the builder
     */
    public static WhenExpressionIsTrueBuilder whenExpressionIsTrue(String expression) {
        return new WhenExpressionIsTrueBuilder().identifier(expression);
    }

    /**
     * A builder of a node that logs the current variables, response and body.
     *
     * @return the builder
     */
    public static ConsoleBuilder console() {
        return new ConsoleBuilder();
    }
}
