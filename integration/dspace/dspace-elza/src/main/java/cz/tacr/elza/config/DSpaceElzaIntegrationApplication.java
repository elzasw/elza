/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.tacr.elza.config;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class DSpaceElzaIntegrationApplication extends ResourceConfig {

    public DSpaceElzaIntegrationApplication() {
        register(JacksonFeature.class);
        packages("cz.tacr.elza", "org.dspace");
    }
}
