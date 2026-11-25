package io.descoped.dc.server.ssl;

import io.descoped.dc.application.spi.Component;
import io.descoped.dc.application.ssl.BusinessSSLResourceSupplier;

public class BusinessSSLResourceComponent implements Component {

    private final BusinessSSLResourceSupplier businessSSLResourceSupplier;

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
    public <R> R getDelegate() {
        return (R) businessSSLResourceSupplier;
    }

    @Override
    public void close() throws Exception {
        // the supplier's resources are closed by its user
    }
}
