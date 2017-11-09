package cz.tacr.elza.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.ClassUtils;
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

    private Session session;

    H2RecursiveQueryBuilder(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public H2RecursiveQueryBuilder<T> addSqlPart(String sqlPart) {
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
        Validate.isTrue(this.session == null);

        this.session = session;
    }

    @Override
    public void setParameter(String name, Object value) {
        Validate.isTrue(nativeQuery == null);

        if (parameterMap.putIfAbsent(name, value) != null) {
            throw new IllegalStateException("Parameter already defined, name:" + name);
        }
    }

    @Override
    public NativeQuery<T> getQuery() {
        if (nativeQuery == null) {
            nativeQuery = buildQuery();
        }
        return nativeQuery;
    }

    private NativeQuery<T> buildQuery() {
        Validate.notNull(session, "prepareQuery must be called first");

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
                throw new IllegalArgumentException("JDBC query has less paramters than specified");
            }
            sb.append(jdbcQuery, offset, index);
            sb.append(it.next());
            offset = index + 1;
        }

        if (jdbcQuery.indexOf('?', offset) >= 0) {
            throw new IllegalArgumentException("JDBC query has more paramters than specified");
        }

        sb.append(jdbcQuery, offset, jdbcQuery.length());

        return session.createNativeQuery(sb.toString(), entityClass);
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
        public Object next() {
            Object value;

            if (hasParamIteratorNext()) {
                value = paramIterator.next();
            } else {
                value = getNextParamValue();
            }

            return resolveSqlValue(value);
        }

        private boolean hasParamIteratorNext() {
            return paramIterator != null && paramIterator.hasNext();
        }

        private Object getNextParamValue() {
            Object value = paramValues[++paramIndex];

            if (value instanceof Collection) {
                paramIterator = ((Collection<?>) value).iterator();
                return next();
            }

            return value;
        }

        private Object resolveSqlValue(Object value) {
            Validate.notNull(value);

            Class<?> valueType = value.getClass();
            if (ClassUtils.isPrimitiveOrWrapper(valueType) || valueType == String.class) {
                return value;
            }
            try {
                return session.getIdentifier(value);
            } catch (TransientObjectException e) {
                // not entity
            }
            throw new IllegalArgumentException("Uknown SQL parameter value:" + value);
        }
    }
}
