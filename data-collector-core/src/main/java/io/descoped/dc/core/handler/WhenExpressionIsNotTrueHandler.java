package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.node.WhenExpressionIsTrue;
import io.descoped.dc.core.handler.state.ConditionType;

/**
 * Executes a {@code WhenExpressionIsTrue} condition: the until-condition result is the value of the node's
 * expression. It throws {@code IllegalStateException} when the identifier is not an expression.
 */
@Handler(forClass = WhenExpressionIsTrue.class)
public class WhenExpressionIsNotTrueHandler extends AbstractHandler<WhenExpressionIsTrue> {

    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public WhenExpressionIsNotTrueHandler(WhenExpressionIsTrue node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        ExecutionContext output = ExecutionContext.empty();

        final String expression = node.identifier();

        ExpressionLanguage el = new ExpressionLanguage(input);
        if (!el.isExpression(expression)) {
            throw new IllegalStateException("Not a valid expression: " + expression);
        }
        boolean isTrue = (boolean) el.evaluateExpression(expression);

        output.state(ConditionType.UNTIL_CONDITION_RESULT, isTrue);
        return output;
    }
}
