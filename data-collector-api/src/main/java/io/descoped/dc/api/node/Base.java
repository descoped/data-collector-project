package io.descoped.dc.api.node;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// used by all simple nodes
/**
 * The base of every node: type tests and casts.
 */
public interface Base {

    /**
     * Whether the given class is this node's class or a subclass of it ({@code getClass().isAssignableFrom(nodeClass)}
     * in the node implementations, so pass an implementation class, not an interface).
     *
     * @param nodeClass the class to test
     * @return {@code true} if the class matches
     */
    boolean instanceOf(Class<? extends Base> nodeClass);

    /**
     * Casts this node to the given type.
     *
     * @param nodeClass the type
     * @param <R>       the type
     * @return this node
     * @throws ClassCastException if this node is not of the type
     */
    <R extends Base> R cast(Class<R> nodeClass);

    /**
     * Calls {@code then} with this node, cast, if {@link #instanceOf(Class)} matches the class.
     *
     * @param isInstanceOfNodeClass the class to test
     * @param then                  the action
     * @param <R>                   the type
     */
    <R extends Base> void given(Class<R> isInstanceOfNodeClass, Consumer<R> then);

    /**
     * Calls {@code then} with this node, cast, if {@link #instanceOf(Class)} matches the class and
     * {@code andIfNotHandled} is {@code false}, and then sets it to {@code true}.
     *
     * @param isInstanceOfNodeClass the class to test
     * @param andIfNotHandled       the flag that tells whether an earlier call handled the node
     * @param then                  the action
     * @param <R>                   the type
     */
    <R extends Base> void given(Class<R> isInstanceOfNodeClass, AtomicBoolean andIfNotHandled, Consumer<R> then);

}
