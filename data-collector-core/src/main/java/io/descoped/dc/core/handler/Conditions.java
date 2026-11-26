package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.node.Condition;
import io.descoped.dc.core.executor.Executor;
import io.descoped.dc.core.handler.state.ConditionType;

/**
 * Evaluation of conditions.
 */
public class Conditions {

    private Conditions() {
    }

    /**
     * Executes an until-condition and returns its result.
     *
     * @param condition the condition
     * @param context   the context to execute it in
     * @return the condition's result
     */
    public static boolean untilCondition(Condition condition, ExecutionContext context) {
        ExecutionContext untilConditionOutput = Executor.execute(condition, context);
        return untilConditionOutput.state(ConditionType.UNTIL_CONDITION_RESULT);
    }
}
