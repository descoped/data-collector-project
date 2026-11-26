package io.descoped.dc.api.node;

import java.util.List;
import java.util.Set;

/**
 * A node that pages through an API: it executes its targets page by page until its condition holds.
 */
public interface Paginate extends NodeWithId {

    /**
     * The names of the pagination's variables.
     *
     * @return the names
     */
    Set<String> variableNames();

    /**
     * The expression of a pagination variable.
     *
     * @param name the variable name
     * @return the expression, or {@code null} if there is no such variable
     */
    String variable(String name);

    /**
     * Whether the content of each page is added to the content stream.
     *
     * @return {@code true} if the page content is added
     */
    boolean addPageContent();

    /**
     * The name of the variable that gives the position of the page content.
     *
     * @return the variable name, or {@code null} if page content is not added
     */
    String positionVariable();

    /**
     * The nodes executed for each page.
     *
     * @return the targets
     */
    List<Execute> targets();

    /**
     * The prefetch threshold: the next page is fetched when fewer positions than this are pending.
     *
     * @return the threshold
     */
    int threshold();

    /**
     * The condition that ends the pagination.
     *
     * @return the condition
     */
    Condition condition();

}
