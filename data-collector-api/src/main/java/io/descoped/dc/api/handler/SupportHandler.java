package io.descoped.dc.api.handler;

import io.descoped.dc.api.node.Base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers the annotated class as a support handler of a node type for a selector class, for example the
 * document parser of a query type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SupportHandler {
    /**
     * The node type.
     *
     * @return the node type
     */
    Class<? extends Base> forClass();

    /**
     * The selector class: the feature the support handler implements.
     *
     * @return the selector class
     */
    Class<?> selectorClass();
}
