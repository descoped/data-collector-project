package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/*
 * Handlers.USE_BYTE_BUDDY is false, so no handler is wrapped; the interceptor methods are called here directly.
 */
class ExecutionWrapperTest {

    @Test
    void thatInterceptInvokesTheMethod() throws ReflectiveOperationException {
        Method empty = ExecutionContext.class.getMethod("empty");

        assertNotNull(ExecutionWrapper.intercept(empty, this, ExecutionContext.empty()));
    }

    @Test
    void thatExecuteGivesNothing() throws ReflectiveOperationException {
        Method empty = ExecutionContext.class.getMethod("empty");

        assertNull(ExecutionWrapper.execute(empty, this, ExecutionContext.empty()));
    }
}
