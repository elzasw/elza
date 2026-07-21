package cz.tacr.elza.common.db;

import java.util.Objects;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionImplementor;

import jakarta.persistence.EntityManager;

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
    },
    POSTGRESQL {
        @Override
        public <T> RecursiveQueryBuilder<T> createRecursiveQueryBuilder(Class<T> entityClass) {
            return new StandardRecursiveQueryBuilder<>(entityClass);
        }
    };

    private static DatabaseType currentDbType;

    public int getMaxInClauseSize() {
        return 1000;
    }

    public abstract <T> RecursiveQueryBuilder<T> createRecursiveQueryBuilder(Class<T> entityClass);

    public static DatabaseType getCurrent() {
        return Objects.requireNonNull(currentDbType, "Not initialized");
    }

    public static boolean isPostgres() {
    	return currentDbType == POSTGRESQL;
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

        Objects.requireNonNull(dialect);

        if (dialect instanceof SQLServerDialect) {
            currentDbType = DatabaseType.MSSQL;
        } else if (dialect instanceof H2Dialect) {
            currentDbType = DatabaseType.H2;
        } else if (dialect instanceof PostgreSQLDialect) {
            currentDbType = DatabaseType.POSTGRESQL;
        } else {
            currentDbType = DatabaseType.GENERIC;
        }
    }
}
