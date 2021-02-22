package cz.tacr.elza.common.db;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class OnPostgreSQLCondition extends AnyNestedCondition {

    public OnPostgreSQLCondition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);

    }

    @ConditionalOnProperty(name = "spring.jpa.database-platform", havingValue = "org.hibernate.dialect.PostgreSQL95Dialect")
    static class OnPostgreSQL95DialectCondition {
    }

    @ConditionalOnProperty(name = "spring.jpa.database-platform", havingValue = "org.hibernate.dialect.PostgreSQL10Dialect")
    static class OnPostgreSQL10DialectCondition {
    }

    @ConditionalOnProperty(name = "spring.jpa.database-platform", havingValue = "org.hibernate.spatial.dialect.postgis.PostgisDialect")
    static class OnPostgisDialectCondition {
    }

    @ConditionalOnProperty(name = "spring.jpa.properties.hibernate.dialect", havingValue = "org.hibernate.spatial.dialect.postgis.PostgisDialect")
    static class OnPostgisDialectCondition2 {
    }
}
