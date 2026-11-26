package io.descoped.dc.application.ssl;

import io.descoped.dc.api.security.BusinessSSLResource;

import java.util.function.Supplier;

/**
 * Holds the supplier of the business certificate resource the workers use.
 */
public class BusinessSSLResourceSupplier {

    private final Supplier<? extends BusinessSSLResource> sslResourceSupplier;

    /**
     * Wraps a supplier.
     *
     * @param sslResourceSupplier the supplier of the resource
     */
    public BusinessSSLResourceSupplier(Supplier<? extends BusinessSSLResource> sslResourceSupplier) {
        this.sslResourceSupplier = sslResourceSupplier;
    }

    /**
     * The supplier.
     *
     * @return the supplier of the resource
     */
    public Supplier<? extends BusinessSSLResource> get() {
        return sslResourceSupplier;
    }
}
