package io.descoped.dc.api.el;

import io.descoped.dc.api.ConfigurationMap;
import io.descoped.dc.api.content.EvaluateLastContentStreamPosition;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.util.CommonUtils;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionLanguage {

    private static final Logger LOG = LoggerFactory.getLogger(ExpressionLanguage.class);

    static final Pattern EXPRESSION_REGEX = Pattern.compile("\\$\\{([^}]+)}");
    static final Pattern MULTI_EXPRESSION_REGEX = Pattern.compile("\\$\\{(.*?)}+");

    private final ConfigurationMap configuration;
    private final Map<String, Object> variables;
    private final EvaluateLastContentStreamPosition evaluateLastContentStreamPosition;

    /**
     * The prefix of the configuration keys that expressions read as {@code ENV}, without the prefix.
     */
    public static final String ENV_PREFIX = "spec.";

    private static final Set<String> BUILT_IN_NAMES = Set.of("ENV", "cast", "convert", "contentStream");

    // The sandbox: a closed world where only what is listed here may be called (JexlPermissions.create denies the
    // rest), and no new(), imports, pragmas or other scripting. The values of variables are strings, numbers,
    // booleans and collections (java.util, whose classes an expression can only get from a variable, since new() and
    // class access are off; java.util.* also matches its subpackages, which are closed again one by one); the bound objects are cast, convert, contentStream and the registered functions. A call of
    // anything else is not made: as with navigation through null (JEXL's safe default, as in 2.x), it gives null.
    static final String ALLOWED = """
            java.lang { +String {} +CharSequence {} +Number {} +Integer {} +Long {} +Short {} +Byte {} +Double {} +Float {} +Boolean {} +Character {} }
            java.math { +BigInteger {} +BigDecimal {} }
            java.util.*
            java.util.concurrent {} java.util.concurrent.atomic {} java.util.concurrent.locks {} java.util.function {}
            java.util.jar {} java.util.logging {} java.util.prefs {} java.util.random {} java.util.regex {} java.util.spi {}
            java.util.stream {} java.util.zip {}
            io.descoped.dc.api.el { +ELCast {} +ELConvert {} +ELContentStream {} }
            """;

    private static class Jexl {
        private static final Map<String, Object> functions;
        private static final IllegalStateException failure;

        static {
            Map<String, Object> loaded = Map.of();
            IllegalStateException failed = null;
            try {
                loaded = loadFunctions();
            } catch (IllegalStateException e) {
                failed = e;
            }
            functions = loaded;
            failure = failed;
        }

        private static final JexlEngine jexlEngine = new JexlBuilder()
                .cache(512)
                .strict(true)
                .silent(false)
                .permissions(new JexlPermissions.ClassPermissions(JexlPermissions.create(ALLOWED),
                        functions.values().stream().map(Object::getClass).toArray(Class<?>[]::new)))
                .features(new JexlFeatures()
                        .newInstance(false)
                        .importPragma(false)
                        .namespacePragma(false)
                        .pragma(false)
                        .annotation(false)
                        .loops(false)
                        .sideEffectGlobal(false)
                        .script(false))
                .create();

        // the providers' problem is reported on every evaluation, not only on the first
        static JexlEngine engine() {
            if (failure != null) {
                throw new IllegalStateException(failure.getMessage(), failure);
            }
            return jexlEngine;
        }

        static Map<String, Object> functions() {
            return functions;
        }

        private static Map<String, Object> loadFunctions() {
            Map<String, Object> loaded = new LinkedHashMap<>();
            for (ExpressionFunctions provider : ServiceLoader.load(ExpressionFunctions.class, ExpressionLanguage.class.getClassLoader())) {
                String name = provider.name();
                if (!isIdentifier(name)) {
                    throw new IllegalStateException("Expression functions name is not an identifier: " + name);
                }
                if (BUILT_IN_NAMES.contains(name) || loaded.containsKey(name)) {
                    throw new IllegalStateException("Expression functions name is taken: " + name);
                }
                loaded.put(name, provider.functions());
            }
            return Collections.unmodifiableMap(loaded);
        }

        // an expression can call ${name.method()} only when the name is an identifier
        static boolean isIdentifier(String name) {
            return name != null && !name.isEmpty() && Character.isJavaIdentifierStart(name.charAt(0))
                    && name.chars().skip(1).allMatch(Character::isJavaIdentifierPart);
        }
    }

    public ExpressionLanguage(ExecutionContext context) {
        this.configuration = context.services().get(ConfigurationMap.class);
        this.variables = new LinkedHashMap<>(context.variables());
        this.evaluateLastContentStreamPosition = new EvaluateLastContentStreamPosition(context);
    }

    // doc: http://commons.apache.org/proper/commons-jexl/reference/examples.html
    void initializeJexlFunctions(JexlContext jexlContext) {
        if (configuration != null) {
            jexlContext.set("ENV", environment(configuration.asMap()));
        }
        Jexl.functions().forEach(jexlContext::set);
        jexlContext.set("cast", new ELCast());
        jexlContext.set("convert", new ELConvert());
        jexlContext.set("contentStream", new ELContentStream(evaluateLastContentStreamPosition));
    }

    // the configuration keys under ENV_PREFIX, without the prefix, as dynamic-configuration's subset("spec") gives them
    static Map<String, String> environment(Map<String, String> configuration) {
        Map<String, String> environment = new LinkedHashMap<>();
        configuration.forEach((key, value) -> {
            if (key.startsWith(ENV_PREFIX) && key.length() > ENV_PREFIX.length()) {
                environment.put(key.substring(ENV_PREFIX.length()), value);
            }
        });
        return Collections.unmodifiableMap(environment);
    }

    public boolean isExpression(String expr) {
        if (expr == null) return false;
        Matcher m = EXPRESSION_REGEX.matcher(expr);
        return m.find();
    }

    public String getExpression(String expr) {
        Matcher m = EXPRESSION_REGEX.matcher(expr);
        if (m.find()) {
            return m.group(1);
        }
        return expr;
    }

    public Object evaluateExpression(String expr) {
        AtomicReference<String> expressionRef = new AtomicReference<>();
        try {
            final String expression = (isExpression(expr) ? getExpression(expr) : expr);
            expressionRef.set(expression);
            JexlExpression e = Jexl.engine().createExpression(expression);
            JexlContext jexlContext = new MapContext(variables);
            initializeJexlFunctions(jexlContext);
            return e.evaluate(jexlContext);
        } catch (RuntimeException | Error e) {
            LOG.error("Error evaluating expr: '{}', exprKey: '{}' with variables: {}\n{}", expr, expressionRef.get(), variables.keySet(), CommonUtils.captureStackTrace(e));
            throw e;
        } catch (Exception e) {
            LOG.error("Error evaluating expr: '{}', exprKey: '{}' with variables: {}\n{}", expr, expressionRef.get(), variables.keySet(), CommonUtils.captureStackTrace(e));
            throw new EvaluationException(e);
        }
    }

    public String evaluateExpressions(String expr) {
        Matcher m = MULTI_EXPRESSION_REGEX.matcher(expr);

        StringBuffer buf = new StringBuffer();
        int last = 0;
        while (m.find()) {
            for (int n = 0; n < m.groupCount(); n++) {
                String group = m.group(n);
                int current = m.start();
                String prev = expr.substring(last, current);
                buf.append(prev);
                buf.append(evaluateExpression(group));
                last = m.end();
            }
        }
        buf.append(expr.substring(last));
        return buf.toString();
    }

}
