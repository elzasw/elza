package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder; TODO hibernate search 6

/**
 * Není v intervalu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class NotIntervalDescItemCondition<T extends Interval<IV>, IV> extends IntervalDescItemCondition<T,IV> {

    public NotIntervalDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }

//    @Override TODO hibernate search 6
//    public Query createLuceneQuery(QueryBuilder queryBuilder) {
//        Query query = super.createLuceneQuery(queryBuilder);
//
//        return queryBuilder.bool().must(query).not().createQuery();
//    }
}
