package io.descoped.dc.application.spi;

import io.descoped.dc.api.http.Request;
import io.undertow.server.HttpHandler;

import java.util.Set;

/**
 * A controller of the application: an HTTP handler for the requests under its context path.
 */
public interface Controller extends HttpHandler {

    /**
     * The path the controller serves, such as {@code /tasks}.
     *
     * @return the context path
     */
    String contextPath();

    /**
     * The HTTP methods the controller serves.
     *
     * @return the methods
     */
    Set<Request.Method> allowedMethods();

}
