package io.descoped.dc.application.engine;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Renders FreeMarker templates from the class path.
 */
public class FreemarkerTemplateEngine {

    private final String basePackagePath;
    private final Configuration config;

    /**
     * Creates the engine.
     *
     * @param basePackagePath the class path location of the templates
     */
    public FreemarkerTemplateEngine(String basePackagePath) {
        this.basePackagePath = basePackagePath;
        config = configure();
    }

    static ClassLoader tccl() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader == null) {
            return ClassLoader.getSystemClassLoader();
        }
        return contextClassLoader;
    }

    Configuration configure() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_29);
        configuration.setClassLoaderForTemplateLoading(tccl(), basePackagePath);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setFallbackOnNullLoopVariable(false);
        return configuration;
    }

    /**
     * Renders a template.
     *
     * @param templateFile the template's file name
     * @param dataModel    the template's data
     * @param output       where the result is written
     */
    public void process(String templateFile, Map<String, Object> dataModel, Writer output) {
        try {
            Template template = config.getTemplate(templateFile);
            template.process(dataModel, output);

        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
