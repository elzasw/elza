package cz.tacr.elza.common.db;

import org.hibernate.Session;

public class MSSQLRecursiveQueryBuilder<T> extends StandardRecursiveQueryBuilder<T> {

    private static final String GENERIC_RECURSIVE_PREFIX = "WITH RECURSIVE";

    private static final String MSSQL_RECURSIVE_PREFIX = "WITH";

    MSSQLRecursiveQueryBuilder(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public void prepareQuery(Session session) {
        String src = sb.toString().toUpperCase();

        int offset = 0;
        while (true) {
            offset = src.indexOf(GENERIC_RECURSIVE_PREFIX, offset);
            if (offset < 0) {
                break;
            }
            sb.replace(offset, offset + GENERIC_RECURSIVE_PREFIX.length(), MSSQL_RECURSIVE_PREFIX);
            offset += MSSQL_RECURSIVE_PREFIX.length();
        }

        super.prepareQuery(session);
    }
}
