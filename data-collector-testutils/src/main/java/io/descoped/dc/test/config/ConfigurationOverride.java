package io.descoped.dc.test.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration keys and values that override the profile's configuration for a test class or method.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationOverride {
    /**
     * The overrides, as alternating keys and values.
     *
     * @return the keys and values
     */
    String[] value() default {};
}
