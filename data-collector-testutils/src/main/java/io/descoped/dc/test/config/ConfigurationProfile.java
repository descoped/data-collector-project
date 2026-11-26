package io.descoped.dc.test.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The configuration profile the test server of a test class or method is started with.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationProfile {
    /**
     * The profile's name.
     *
     * @return the name; {@code test} by default
     */
    String value() default "test";
}
