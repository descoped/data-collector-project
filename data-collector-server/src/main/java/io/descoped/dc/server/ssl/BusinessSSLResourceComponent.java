package io.descoped.dc.server.ssl;

import io.descoped.dc.application.spi.Component;
import io.descoped.dc.application.ssl.BusinessSSLResourceSupplier;

/**
 * The business certificate resource as a component of the application.
 */
public class BusinessSSLResourceComponent implements Component {

    private final BusinessSSLResourceSupplier businessSSLResourceSupplier;

    /**
     * Creates the component.
     *
     * @param businessSSLResourceSupplier the resource's supplier
     */
    public BusinessSSLResourceComponent(BusinessSSLResourceSupplier businessSSLResourceSupplier) {
        this.businessSSLResourceSupplier = businessSSLResourceSupplier;
    }

    @Override
    public void initialize() {
        // the supplier is given at construction
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked") // the caller names the delegate's type
    public <R> R getDelegate() {
        return (R) businessSSLResourceSupplier;
    }

    @Override
    public void close() {
        // the supplier's resources are closed by its user
    }
}
