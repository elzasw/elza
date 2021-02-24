package cz.tacr.elza.common.db;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class OnH2Condition extends AnyNestedCondition {

    public OnH2Condition() {
        super(ConfigurationPhase.PARSE_CONFIGURATION);

    }

    @ConditionalOnProperty(name = "spring.jpa.database-platform", havingValue = "org.hibernate.dialect.H2Dialect")
    static class OnH2DialectCondition {
    }

    @ConditionalOnProperty(name = "spring.jpa.database-platform", havingValue = "org.hibernate.spatial.dialect.h2geodb.GeoDBDialect")
    static class OnH2GeoDialectCondition {
    }

    @ConditionalOnProperty(name = "spring.jpa.properties.hibernate.dialect", havingValue = "org.hibernate.spatial.dialect.h2geodb.GeoDBDialect")
    static class OnH2GeoDialect2Condition {
    }
}
