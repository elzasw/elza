package cz.tacr.elza.hibernate;

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

    public HibernateConfiguration(DataSource dataSource,
			JpaProperties jpaProperties,
			ObjectProvider<JtaTransactionManager> jtaTransactionManager,
			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		super(dataSource, jpaProperties, jtaTransactionManager,
				transactionManagerCustomizers);
	}

    @Override
    protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
        vendorProperties.put("hibernate.session_factory.interceptor", StaticDataTransactionInterceptor.INSTANCE);
        super.customizeVendorProperties(vendorProperties);
    }
}
