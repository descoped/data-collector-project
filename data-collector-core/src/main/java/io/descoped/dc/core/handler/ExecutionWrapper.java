package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class ExecutionWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionWrapper.class);

    private ExecutionWrapper() {
    }

    @RuntimeType
    public static ExecutionContext intercept(@Origin Method method, @This Object self, @Argument(0) ExecutionContext input) throws ReflectiveOperationException {
        LOG.info("intercepted");
        return (ExecutionContext) method.invoke(input);
    }

    @RuntimeType
    public static ExecutionContext execute(@Origin Method method, @This Object self, @Argument(0) ExecutionContext input) {
        LOG.info("Intercepted");
        return null;
    }

}
