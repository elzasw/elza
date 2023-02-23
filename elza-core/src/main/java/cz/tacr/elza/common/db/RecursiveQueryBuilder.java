package cz.tacr.elza.common.db;

import jakarta.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

/**
 * Interface to build recursive query
 *
 * Usage: call addSqlPart to add single or multiple query parts, recursive query
 * have to be inserted in PostgreSQL/Oracle syntax. It means using 'WITH
 * RECURSIVE data(..) as ...'
 *
 * When all parts are added to the builder prepareQuery have to be called.
 *
 * Next step is to add parameters with method setParameter.
 *
 * Last step is to get prepared query. There are two methods: getQuery and
 * getNativeQuery.
 *
 * @param <T>
 */
public interface RecursiveQueryBuilder<T> {

    RecursiveQueryBuilder<T> addSqlPart(String sqlPart);

    void prepareQuery(EntityManager em);

    void prepareQuery(Session session);

    void setParameter(String name, Object value);

	/**
	 * Return type safe native query
	 *
	 * @return
	 */
    NativeQuery<T> getQuery();

	/**
	 * Return untyped native query
	 *
	 * @return Return native query (not type safe)
	 */
	@SuppressWarnings("rawtypes")
	NativeQuery getNativeQuery();
}
