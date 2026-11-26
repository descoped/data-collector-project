package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.QueryFeature;
import io.descoped.dc.api.handler.QueryResult;
import io.descoped.dc.api.handler.QueryState;
import io.descoped.dc.api.node.Base;

import java.util.List;

/**
 * The base of the query handlers: {@link #execute(ExecutionContext)} reads the {@code QueryState} of the input
 * context, evaluates its data as a list, an object or a string literal, and returns the result as the
 * {@code QueryResult} state of a new context. A handler executes once: a second execution throws
 * {@code IllegalStateException}.
 *
 * @param <N> the type of query node the handler executes
 */
public abstract class AbstractQueryHandler<N extends Base> extends AbstractHandler<N> implements QueryFeature {

    private ExecutionContext context;

    /**
     * Creates a handler for a query node.
     *
     * @param node the query node to execute
     */
    protected AbstractQueryHandler(N node) {
        super(node);
    }

    /**
     * The context this handler was executed with.
     *
     * @return the context, or {@code null} before {@link #execute(ExecutionContext)}
     */
    protected ExecutionContext context() {
        return context;
    }

    // Xpath and JqPath expression evaluation
    /**
     * Evaluates the expressions in a query expression against the variables of the context.
     *
     * @param expression the query expression
     * @return the expression with its expressions evaluated, or the expression itself when it has none
     */
    protected String evaluateExpression(String expression) {
        ExpressionLanguage el = new ExpressionLanguage(context());
        if (el.isExpression(expression)) {
            return el.evaluateExpressions(expression);
        }
        return expression;
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        // guard against mutated state
        if (context != null) {
            throw new IllegalStateException("An ExecutionContext has already been assigned. It is illegal to mutate state!");
        }
        this.context = input;

        QueryState<?> queryState = input.state(QueryState.class);
        if (queryState == null) {
            throw new IllegalStateException("QueryState is NOT set in ExecutionContext.state()");
        }

        if (queryState.type() == Type.LIST) {
            List<?> nodeList = evaluateList(queryState.data());
            return ExecutionContext.empty().state(QueryResult.class, new QueryResult<>(nodeList));

        } else if (queryState.type() == Type.OBJECT) {
            Object node = evaluateObject(queryState.data());
            return ExecutionContext.empty().state(QueryResult.class, new QueryResult<>(node));


        } else if (queryState.type() == Type.STRING_LITERAL) {
            String literal = evaluateStringLiteral(queryState.data());
            return ExecutionContext.empty().state(QueryResult.class, new QueryResult<>(literal));
        }

        return ExecutionContext.empty();
    }
}
