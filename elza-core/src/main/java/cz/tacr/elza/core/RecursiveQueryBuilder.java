package cz.tacr.elza.core;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

public interface RecursiveQueryBuilder<T> {

    RecursiveQueryBuilder<T> addSqlPart(String sqlPart);

    void prepareQuery(EntityManager em);

    void prepareQuery(Session session);

    void setParameter(String name, Object value);

    NativeQuery<T> getQuery();
}
