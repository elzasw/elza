package cz.tacr.elza.common.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.query.NativeQuery;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class H2RecursiveQueryBuilder<T> implements RecursiveQueryBuilder<T> {

    private final Map<String, Object> parameterMap = new HashMap<>();

    private final StringBuilder sb = new StringBuilder();

    private final Class<T> entityClass;

    private NativeQuery<T> nativeQuery;

	/**
	 * Raw native query
	 */
	@SuppressWarnings("rawtypes")
	private NativeQuery rawNativeQuery;

    private Session session;

    H2RecursiveQueryBuilder(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public H2RecursiveQueryBuilder<T> addSqlPart(String sqlPart) {
		Validate.isTrue(rawNativeQuery == null);

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
        Validate.isTrue(this.session == null);

        this.session = session;
    }

    @Override
    public void setParameter(String name, Object value) {
		Validate.isTrue(rawNativeQuery == null);

        if (parameterMap.putIfAbsent(name, value) != null) {
            throw new IllegalStateException("Parameter already defined, name:" + name);
        }
    }

	@SuppressWarnings("unchecked")
	@Override
    public NativeQuery<T> getQuery() {
		Validate.notNull(session, "prepareQuery must be called first");

		if (rawNativeQuery == null) {
			buildQuery();
		}
		Validate.notNull(rawNativeQuery);
		if (nativeQuery != null) {
			return nativeQuery;
		} else {
			return rawNativeQuery;
		}
    }

	@SuppressWarnings("rawtypes")
	@Override
	public NativeQuery getNativeQuery() {
		Validate.notNull(session, "prepareQuery must be called first");

		if (rawNativeQuery == null) {
			buildQuery();
		}
		Validate.notNull(rawNativeQuery);
		return rawNativeQuery;
	}

	/**
	 * Prepare pure JPA/HQL query and substitue all parameters
	 *
	 * @return Return pure JPA/HQL query
	 */
	private String prepareSqlQuery() {

        // locate named parameters in query
        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sb.toString());

        // create JDBC parameterized query with expanded collections
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource(parameterMap);
        String jdbcQuery = NamedParameterUtils.substituteNamedParameters(parsedSql, sqlParameterSource);

        // create value array from parameters with order of parameterized JDBC query
        Object[] valueArray = NamedParameterUtils.buildValueArray(parsedSql, sqlParameterSource, null);

        // reset internal string builder
        sb.setLength(0);

        // replace all question marks with values and append result to string builder
        SqlParamValuesIterator it = new SqlParamValuesIterator(valueArray, session);
        int offset = 0;

        while (it.hasNext()) {
            int index = jdbcQuery.indexOf('?', offset);
            if (index < 0) {
                throw new IllegalArgumentException("JDBC query has less parameters than specified");
            }
            sb.append(jdbcQuery, offset, index);
            sb.append(it.next());
            offset = index + 1;
        }

        if (jdbcQuery.indexOf('?', offset) >= 0) {
            throw new IllegalArgumentException("JDBC query has more parameters than specified");
        }

        sb.append(jdbcQuery, offset, jdbcQuery.length());

		return sb.toString();
	}

	private void buildQuery() {
		String sqlString = prepareSqlQuery();

		// check if entity type is not defined
		// or if it is primitive type
		if (StandardRecursiveQueryBuilder.isEntityClass(session, entityClass)) {
			nativeQuery = session.createNativeQuery(sqlString, entityClass);
			rawNativeQuery = nativeQuery;
		} else {
			rawNativeQuery = session.createNativeQuery(sqlString);
		}
    }

    private static class SqlParamValuesIterator implements Iterator<Object> {

        private final Object[] paramValues;

        private final Session session;

        private int paramIndex = -1;

        private Iterator<?> paramIterator;

        public SqlParamValuesIterator(Object[] paramValues, Session session) {
            this.paramValues = paramValues;
            this.session = session;
        }

        @Override
        public boolean hasNext() {
            return paramIndex + 1 < paramValues.length || hasParamIteratorNext();
        }

        @Override
        public String next() {
            Object paramValue;

            if (hasParamIteratorNext()) {
                paramValue = paramIterator.next();
            } else {
                paramValue = getNextParamValue();
            }

            return resolveSqlValue(paramValue);
        }

        private boolean hasParamIteratorNext() {
            return paramIterator != null && paramIterator.hasNext();
        }

        private Object getNextParamValue() {
            Object value = paramValues[++paramIndex];

            if (value instanceof Collection) {
                paramIterator = ((Collection<?>) value).iterator();
                value = paramIterator.next(); // empty collection must be exception
            }

            return value;
        }

        private String resolveSqlValue(Object value) {
            Validate.notNull(value);

            Class<?> javaType = value.getClass();

            if (javaType == boolean.class || javaType == Boolean.class) {
                return value.toString();
            }
            if (javaType == byte.class || javaType == Byte.class) {
                return value.toString();
            }
            if (javaType == int.class || javaType == Integer.class) {
                return value.toString();
            }
            if (javaType == double.class || javaType == Double.class) {
                return value.toString();
            }
            if (javaType == java.sql.Timestamp.class) {
                return '\'' + value.toString() + '\'';
            }
            if (javaType == String.class) {
                return '\'' + value.toString() + '\'';
            }
            if (javaType.isEnum()) {
                return '\'' + ((Enum<?>) value).name() + '\'';
            }

            // try to convert entity
            try {
                return session.getIdentifier(value).toString();
            } catch (TransientObjectException e) {
                // not entity
            }

            throw new IllegalArgumentException(
                    "Uknown SQL parameter, class: " + javaType.getName() + ", value:" + value);
        }
    }
}
