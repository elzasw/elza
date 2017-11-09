package cz.tacr.elza.core;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

public class StandardRecursiveQueryBuilder<T> implements RecursiveQueryBuilder<T> {

    protected final StringBuilder sb = new StringBuilder();

    protected final Class<T> entityClass;

    private NativeQuery<T> nativeQuery;

    StandardRecursiveQueryBuilder(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public RecursiveQueryBuilder<T> addSqlPart(String sqlPart) {
        Validate.isTrue(nativeQuery == null);

        sb.append(sqlPart);
        return this;
    }

    @Override
    public void prepareQuery(EntityManager em) {
        Session session = em.unwrap(Session.class);
        prepareQuery(session);
    }

    @Override
    public void prepareQuery(Session session) {
        Validate.isTrue(nativeQuery == null);

        String sqlString = sb.toString();
        nativeQuery = session.createNativeQuery(sqlString, entityClass);
    }

    @Override
    public void setParameter(String name, Object value) {
        nativeQuery.setParameter(name, value);
    }

    @Override
    public NativeQuery<T> getQuery() {
        Validate.notNull(nativeQuery, "prepareQuery must be called first");

        return nativeQuery;
    }
}
