package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;

/**
 * Neobsahuje.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @param <T>
 * @since 14. 4. 2016
 */
public class NotContainDescItemCondition<T> extends ContainDescItemCondition<T>{

    public NotContainDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }

    @Override
    public Query createLuceneQuery(QueryBuilder queryBuilder) {
        Query query = super.createLuceneQuery(queryBuilder);

        return queryBuilder.bool().must(query).not().createQuery();
    }
}
