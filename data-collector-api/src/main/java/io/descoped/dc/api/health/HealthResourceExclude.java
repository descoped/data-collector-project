package io.descoped.dc.api.health;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Keeps a health resource from being created when the health resources are loaded: for a resource that is created
 * and added later, under an id.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface HealthResourceExclude {
}
