package cz.tacr.elza.core;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.engine.spi.SessionImplementor;

import cz.tacr.elza.service.StartupService;

/**
 * Typ Databáze. Zatím existuje pouze 1 specialita pro MSSQL.
 */
public enum DatabaseType {
    GENERIC {
        @Override
        public String getRecursiveQueryPrefix() {
            return "WITH RECURSIVE";
        }
    },
    MSSQL {
        @Override
        public String getRecursiveQueryPrefix() {
            return "WITH";
        }
    };

    private static DatabaseType currentDbType;

    public abstract String getRecursiveQueryPrefix();

    public int getMaxInClauseSize() {
        return 1000;
    }

    public static DatabaseType getCurrent() {
        return Validate.notNull(currentDbType, "Not initialized");
    }

    /**
     * Initialized during application startup.
     *
     * @see StartupService#start()
     */
    public static void init(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        SessionImplementor si = session.unwrap(SessionImplementor.class);
        Dialect dialect = si.getJdbcServices().getDialect();

        Validate.notNull(dialect);

        if (dialect instanceof SQLServer2008Dialect) {
            currentDbType = DatabaseType.MSSQL;
        } else {
            currentDbType = DatabaseType.GENERIC;
        }
    }
}
