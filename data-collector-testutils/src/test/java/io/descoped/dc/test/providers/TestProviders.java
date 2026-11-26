package io.descoped.dc.test.providers;

import io.descoped.dc.api.Processor;
import io.descoped.dc.api.ProcessorProvider;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionFunctions;

/**
 * Providers registered in this module's test resources, as a deployment registers its own.
 */
public final class TestProviders {

    private TestProviders() {
    }

    /**
     * A processor that sets {@code next} to {@code 42}.
     */
    public static class NextPageProcessor implements Processor {
        @Override
        public ExecutionContext process(ExecutionContext input) {
            return ExecutionContext.empty().variable("next", "42");
        }
    }

    /**
     * Registers {@link NextPageProcessor} as {@code test-next-page}.
     */
    public static class NextPageProcessorProvider implements ProcessorProvider {
        @Override
        public String name() {
            return "test-next-page";
        }

        @Override
        public Class<? extends Processor> processorClass() {
            return NextPageProcessor.class;
        }
    }

    /**
     * Functions a specification calls as {@code sign.*}.
     */
    public static class Signing {
        public String hmac(String value) {
            return "signed:" + value;
        }
    }

    /**
     * Registers {@link Signing} as {@code sign}.
     */
    public static class SigningFunctions implements ExpressionFunctions {
        @Override
        public String name() {
            return "sign";
        }

        @Override
        public Object functions() {
            return new Signing();
        }
    }
}
