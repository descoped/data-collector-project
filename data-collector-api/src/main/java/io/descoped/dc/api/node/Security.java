package io.descoped.dc.api.node;

import java.util.List;

/**
 * The specification's security configuration.
 */
public interface Security extends Configuration {

    /**
     * The name of the SSL bundle (business certificate) the client uses.
     *
     * @return the bundle name, or {@code null} if none is set
     */
    String sslBundleName();

    /**
     * The identities of the specification.
     *
     * @return the identities
     */
    List<Identity> identities();

}
