package io.descoped.dc.api.handler;

import io.descoped.dc.api.node.Base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers the annotated class as the handler of a node type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Handler {
    /**
     * The node type.
     *
     * @return the node type
     */
    Class<? extends Base> forClass();
}
