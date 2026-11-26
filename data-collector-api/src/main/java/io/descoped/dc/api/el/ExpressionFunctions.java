package io.descoped.dc.api.el;

/**
 * Functions that specification expressions may call, bound under a name: an expression calls
 * {@code ${<name>.<method>(...)}}. This is how a specification reaches code of its own, for example to sign a request
 * for a service with a security scheme of its own, while expressions stay in the sandbox: only the public methods of
 * {@link #functions()}'s class, and the built-in {@code cast}, {@code convert} and {@code contentStream}, can be
 * called.
 *
 * <p>A provider is a service: it is registered with {@code provides} in its {@code module-info.java} and in
 * {@code META-INF/services}, and it has a public constructor without parameters. Providers are loaded once; the
 * functions object is shared by every expression of every worker, so it must be thread-safe. A provider whose name is
 * not an identifier, is taken, or is {@code ENV}, {@code cast}, {@code convert} or {@code contentStream}, stops every
 * expression from being evaluated, with an error that names it.
 *
 * <p>The name hides a context variable of the same name. JEXL calls the methods reflectively, so the package of the
 * functions' class is exported (or opened) to {@code org.apache.commons.jexl3}.
 */
public interface ExpressionFunctions {

    /**
     * The name the functions are bound under in expressions.
     *
     * @return the name, a valid identifier
     */
    String name();

    /**
     * The object whose public methods expressions may call.
     *
     * @return the functions
     */
    Object functions();
}
