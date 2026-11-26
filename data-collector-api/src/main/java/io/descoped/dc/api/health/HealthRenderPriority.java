package io.descoped.dc.api.health;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The order of a health resource in the health output; every health resource needs it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface HealthRenderPriority {
    /**
     * The priority: a lower value is rendered first.
     *
     * @return the priority
     */
    int priority();
}
