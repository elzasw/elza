package cz.tacr.elza.common.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

public class StandardRecursiveQueryBuilder<T> implements RecursiveQueryBuilder<T> {

    protected final StringBuilder sb = new StringBuilder();

    protected final Class<T> entityClass;

	/**
	 * Raw native query
	 */
	@SuppressWarnings("rawtypes")
	private NativeQuery rawNativeQuery;

    private NativeQuery<T> nativeQuery;

    StandardRecursiveQueryBuilder(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public RecursiveQueryBuilder<T> addSqlPart(String sqlPart) {
		Validate.isTrue(rawNativeQuery == null);

        sb.append(sqlPart);
        return this;
    }

    @Override
    public void prepareQuery(EntityManager em) {
        Session session = em.unwrap(Session.class);
        prepareQuery(session);
    }

	/**
	 * Helper method to check if entityClass is real entity class
	 *
	 * @param session
	 * @param entityClass
	 * @return
	 */
    static public boolean isEntityClass(Session session, Class<?> entityClass) {
        if (entityClass == null) {
            return false;
        }
        // use session to check entityClass
        Metamodel metaModel = session.getMetamodel();
        try {
            EntityType<?> type = metaModel.entity(entityClass);
            if (type == null) {
                return false;
            }
        } catch (IllegalArgumentException e) {
            // if not found metamodel throws this exception
            return false;
        }
        return true;
    }

    @Override
    public void prepareQuery(Session session) {
		Validate.isTrue(rawNativeQuery == null);

        String sqlString = sb.toString();

		// check if entity type is not defined
		// or if it is primitive type
		if (isEntityClass(session, entityClass)) {
			nativeQuery = session.createNativeQuery(sqlString, entityClass);
			rawNativeQuery = nativeQuery;
		} else {
			rawNativeQuery = session.createNativeQuery(sqlString);
		}
    }

    @Override
    public void setParameter(String name, Object value) {
		rawNativeQuery.setParameter(name, value);
    }

	@SuppressWarnings("unchecked")
	@Override
    public NativeQuery<T> getQuery() {
		Validate.notNull(rawNativeQuery, "prepareQuery must be called first");
		if (nativeQuery != null) {
			return nativeQuery;
		} else {
			return rawNativeQuery;
		}
    }

	@SuppressWarnings("rawtypes")
	@Override
	public NativeQuery getNativeQuery() {
		Validate.notNull(rawNativeQuery, "prepareQuery must be called first");

		return rawNativeQuery;
	}
}
