package cz.tacr.elza.common.db;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.engine.spi.SessionImplementor;

import cz.tacr.elza.service.StartupService;

/**
 * Typ Databáze. Zatím existuje pouze 1 specialita pro MSSQL.
 */
public enum DatabaseType {
    GENERIC {
        @Override
        public <T> RecursiveQueryBuilder<T> createRecursiveQueryBuilder(Class<T> entityClass) {
            return new StandardRecursiveQueryBuilder<>(entityClass);
        }
    },
    MSSQL {
        @Override
        public <T> RecursiveQueryBuilder<T> createRecursiveQueryBuilder(Class<T> entityClass) {
            return new MSSQLRecursiveQueryBuilder<>(entityClass);
        }
    },
    H2 {
        @Override
        public <T> RecursiveQueryBuilder<T> createRecursiveQueryBuilder(Class<T> entityClass) {
            return new H2RecursiveQueryBuilder<>(entityClass);
        }
    };

    private static DatabaseType currentDbType;

    public int getMaxInClauseSize() {
        return 1000;
    }

    public abstract <T> RecursiveQueryBuilder<T> createRecursiveQueryBuilder(Class<T> entityClass);

    public static DatabaseType getCurrent() {
        return Validate.notNull(currentDbType, "Not initialized");
    }

    /**
     * Initialized during application startup.
     *
     * @see StartupService#
     *
     * start()
     */
    public static void init(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        SessionImplementor si = session.unwrap(SessionImplementor.class);
        Dialect dialect = si.getJdbcServices().getDialect();

        Validate.notNull(dialect);

        if (dialect instanceof SQLServer2008Dialect) {
            currentDbType = DatabaseType.MSSQL;
        } else if (dialect instanceof H2Dialect) {
            currentDbType = DatabaseType.H2;
        } else {
            currentDbType = DatabaseType.GENERIC;
        }
    }
}
