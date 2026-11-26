package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.QueryException;
import io.descoped.dc.api.handler.QueryState;
import io.descoped.dc.api.node.Eval;

import java.util.List;

/**
 * Executes an {@code Eval} query: when the node has a query, it evaluates it on the data and binds the result to the
 * node's variable, then evaluates the node's expression. Only string literal queries are supported.
 */
@Handler(forClass = Eval.class)
public class EvalHandler extends AbstractQueryHandler<Eval> {

    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public EvalHandler(Eval node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        @SuppressWarnings("unchecked") // the state is kept by its raw class
        QueryState<Eval> queryState = input.state(QueryState.class);

        if (queryState == null) {
            throw new IllegalArgumentException("QueryState is not set!");
        }

        if (queryState.type() != Type.STRING_LITERAL) {
            throw new QueryException("Only QueryFeature.Type.STRING_LITERAL is supported!");
        }

        /*
         * execute sub-query and bind variable to output
         */

        if (node.query() != null) {
            String result = Queries.from(input, node.query()).evaluateStringLiteral(queryState.data());
            input.variable(node.bind(), result);
        }

        /*
         * execute this handler
         */

        ExecutionContext evalContext = ExecutionContext.of(input);
        ExpressionLanguage el = new ExpressionLanguage(input);
        evalContext.state(QueryState.class, new QueryState<>(queryState.type(), el));

        return super.execute(evalContext);
    }

    @Override
    public List<?> evaluateList(Object data) {
        throw new UnsupportedOperationException("queryList is not supported!");
    }

    @Override
    public Object evaluateObject(Object data) {
        throw new UnsupportedOperationException("queryObject is not supported!");
    }

    @Override
    public String evaluateStringLiteral(Object data) {
        ExpressionLanguage el = (ExpressionLanguage) data;
        Object value = el.evaluateExpression(node.expression());
        return (value != null ? value.toString() : null);
    }
}
