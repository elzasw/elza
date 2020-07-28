package cz.tacr.elza.core.db;

import cz.tacr.elza.core.data.StaticDataTransactionInterceptor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HibernateConfiguration implements HibernatePropertiesCustomizer {

    /**
     * Batch size
     * 
     * Maximal number of items inside IN clause
     */
    final public static int MAX_IN_SIZE = 1000;

    @Override
    public void customize(final Map<String, Object> vendorProperties) {
        // register static data interceptor
        vendorProperties.put(org.hibernate.cfg.AvailableSettings.INTERCEPTOR, StaticDataTransactionInterceptor.INSTANCE);
        // use enhanced (modern) generators (in JPA is default true -> safety check)
        vendorProperties.put(org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, Boolean.TRUE);
        // set custom provider for id generator strategy
        vendorProperties.put(org.hibernate.jpa.AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER,
                cz.tacr.elza.core.db.IdentifierGeneratorStrategyProvider.class.getName());

    }
}
