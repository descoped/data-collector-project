package io.descoped.dc.api.el;

import io.descoped.dc.api.ConfigurationMap;
import io.descoped.dc.api.context.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Expressions are evaluated in a sandbox: only the allow-list can be called, and ENV holds only the spec.* keys.
 */
class ExpressionSandboxTest {

    static ExpressionLanguage el(Map<String, Object> variables, Map<String, String> configuration) {
        ExecutionContext context = new ExecutionContext.Builder().variables(variables).build();
        context.services().register(ConfigurationMap.class, new ConfigurationMap(configuration));
        return new ExpressionLanguage(context);
    }

    static ExpressionLanguage el() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("position", "41");
        variables.put("name", "abc");
        variables.put("list", List.of("a", "b"));
        Map<String, String> configuration = new LinkedHashMap<>();
        configuration.put("spec.ssb.client.id", "client");
        configuration.put("rawdata.encryption.key", "secret-key");
        configuration.put("http.authentication.bearer.token", "token");
        return el(variables, configuration);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "${''.class.forName('java.lang.Runtime')}",
            "${''.getClass().forName('java.lang.Runtime').getRuntime().exec('id')}",
            "${new('java.lang.ProcessBuilder', 'id')}",
            "${cast.getClass().getClassLoader()}",
            "${contentStream.getClass()}",
            "${name.getClass().forName('java.lang.System').exit(1)}",
            "${list.getClass()}",
            "${ENV.getClass()}",
    })
    void thatClassesAndConstructorsCannotBeReached(String expression) {
        // the call is not made: the expression fails, or gives null as navigation through null does
        try {
            assertNull(el().evaluateExpression(expression), expression);
        } catch (RuntimeException expected) {
            // refused
        }
    }

    @Test
    void thatEnvHoldsOnlyTheSpecKeys() {
        assertEquals("client", el().evaluateExpression("${ENV.'ssb.client.id'}"));
        assertNull(el().evaluateExpression("${ENV.'rawdata.encryption.key'}"));
        assertNull(el().evaluateExpression("${ENV.'spec.ssb.client.id'}"));
        assertNull(el().evaluateExpression("${ENV.'http.authentication.bearer.token'}"));
        assertEquals(Map.of("ssb.client.id", "client"),
                ExpressionLanguage.environment(Map.of("spec.ssb.client.id", "client", "spec.", "x", "other", "y")));
    }

    @Test
    void thatWhatSpecificationsUseStillWorks() {
        assertEquals(42L, el().evaluateExpression("${cast.toLong(position) + 1}"));
        assertEquals(true, el().evaluateExpression("${position == '41'}"));
        assertEquals(3, el().evaluateExpression("${name.length()}"));
        assertEquals("ABC", el().evaluateExpression("${name.toUpperCase()}"));
        assertEquals(2, el().evaluateExpression("${list.size()}"));
        assertEquals("b", el().evaluateExpression("${list[1]}"));
        assertEquals("p-41-client", el().evaluateExpressions("p-${position}-${ENV.'ssb.client.id'}"));
    }
}
