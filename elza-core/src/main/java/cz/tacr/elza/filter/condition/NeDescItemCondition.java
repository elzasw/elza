package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;

/**
 * Není rovno.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class NeDescItemCondition<T> extends EqDescItemCondition<T> {

    public NeDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }

    @Override
    public Query createLuceneQuery(QueryBuilder queryBuilder) {
        Query query = super.createLuceneQuery(queryBuilder);

        return queryBuilder.bool().must(query).not().createQuery();
    }
}
