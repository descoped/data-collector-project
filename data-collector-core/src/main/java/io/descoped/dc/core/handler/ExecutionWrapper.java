package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Byte Buddy interceptor for the {@code execute} method of a handler; {@code Handlers} uses it only when its Byte
 * Buddy switch is on, which it is not.
 */
public class ExecutionWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionWrapper.class);

    private ExecutionWrapper() {
    }

    /**
     * Logs the interception and invokes the intercepted method reflectively on the input context, without arguments.
     *
     * @param method the intercepted method
     * @param self   the intercepted object
     * @param input  the input context
     * @return the method's result
     * @throws ReflectiveOperationException if the method cannot be invoked
     */
    @RuntimeType
    public static ExecutionContext intercept(@Origin Method method, @This Object self, @Argument(0) ExecutionContext input) throws ReflectiveOperationException {
        LOG.info("intercepted");
        return (ExecutionContext) method.invoke(input);
    }

    /**
     * Logs the interception and returns {@code null}.
     *
     * @param method the intercepted method
     * @param self   the intercepted object
     * @param input  the input context
     * @return {@code null}
     */
    @RuntimeType
    public static ExecutionContext execute(@Origin Method method, @This Object self, @Argument(0) ExecutionContext input) {
        LOG.info("Intercepted");
        return null;
    }

}
