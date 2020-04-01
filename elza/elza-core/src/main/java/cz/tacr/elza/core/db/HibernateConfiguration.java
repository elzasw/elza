package cz.tacr.elza.core.db;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

import cz.tacr.elza.core.data.StaticDataTransactionInterceptor;

@Configuration
public class HibernateConfiguration extends HibernateJpaAutoConfiguration {

    /**
     * Batch size
     * 
     * Maximal number of items inside IN clause
     */
    final public static int MAX_IN_SIZE = 1000;

    public HibernateConfiguration(DataSource dataSource,
                                  JpaProperties jpaProperties,
                                  ObjectProvider<JtaTransactionManager> jtaTransactionManager,
                                  ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        super(dataSource, jpaProperties, jtaTransactionManager, transactionManagerCustomizers);
    }

    @Override
    protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
        // register static data interceptor
        vendorProperties.put(org.hibernate.cfg.AvailableSettings.INTERCEPTOR, StaticDataTransactionInterceptor.INSTANCE);
        // use enhanced (modern) generators (in JPA is default true -> safety check)
        vendorProperties.put(org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, Boolean.TRUE);
        // set custom provider for id generator strategy
        vendorProperties.put(org.hibernate.jpa.AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER,
                cz.tacr.elza.core.db.IdentifierGeneratorStrategyProvider.class.getName());

        super.customizeVendorProperties(vendorProperties);
    }
}
