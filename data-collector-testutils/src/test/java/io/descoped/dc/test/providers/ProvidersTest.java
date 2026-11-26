package io.descoped.dc.test.providers;

import io.descoped.dc.api.Builders;
import io.descoped.dc.api.ProcessorProvider;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.node.Process;
import io.descoped.dc.api.node.builder.ProcessBuilder;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.api.Specification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Processors and expression functions are reached by the names their providers register, never by a class name.
 */
class ProvidersTest {

    @Test
    void thatAProcessNamesItsProvider() {
        String json = Builders.process("test-next-page").output("next").serialize();
        assertTrue(json.contains("\"processor\" : \"test-next-page\""), json);

        ProcessBuilder deserialized = Specification.deserialize(json, ProcessBuilder.class);
        Process node = deserialized.build();
        assertEquals(TestProviders.NextPageProcessor.class, node.processorClass());
        assertEquals("test-next-page", node.processor());
        assertEquals(json, deserialized.serialize());
    }

    @Test
    void thatAJavaClassIsWrittenAsItsProvidersName() {
        String json = Builders.process(TestProviders.NextPageProcessor.class).output("next").serialize();
        assertTrue(json.contains("\"processor\" : \"test-next-page\""), json);
        assertEquals(TestProviders.NextPageProcessor.class, ProcessorProvider.find("test-next-page").orElseThrow());
    }

    @Test
    void thatAClassNameInASpecificationIsNotRead() {
        String json = """
                {
                  "type" : "Process",
                  "processorClass" : "io.descoped.dc.test.providers.TestProviders$NextPageProcessor",
                  "requiredOutputs" : [ "next" ]
                }
                """;
        assertThrows(RuntimeException.class, () -> Specification.deserialize(json, ProcessBuilder.class));
    }

    @Test
    void thatAnUnknownProviderFailsWhenTheProcessRuns() {
        Process node = Builders.process("no-such-provider").build();
        assertThrows(IllegalStateException.class, node::processorClass);
    }

    @Test
    void thatExpressionsCallRegisteredFunctionsOnly() {
        ExpressionLanguage el = new ExpressionLanguage(ExecutionContext.empty());
        assertEquals("signed:x", el.evaluateExpression("${sign.hmac('x')}"));
        try {
            assertNull(el.evaluateExpression("${sign.getClass().getClassLoader()}"));
        } catch (RuntimeException _) {
            // refused
        }
    }

    @Test
    void thatAJavaSpecificationWithUnregisteredClassesIsNotWritten() {
        SpecificationBuilder specification = Specification.start("id", "name", "start")
                .function(Builders.get("start").url("http://localhost/").pipe(Builders.process(UnregisteredProcessor.class)));
        assertThrows(RuntimeException.class, specification::serialize);
    }

    static class UnregisteredProcessor extends TestProviders.NextPageProcessor {
    }
}
