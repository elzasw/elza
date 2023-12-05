package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 * Je v intervalu včetně.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class IntervalDescItemCondition<T extends Interval<IV>, IV> extends AbstractDescItemConditionWithValue<T> {

    public IntervalDescItemCondition(final T conditionValue, final String attributeName) {
        super(conditionValue, attributeName);
    }

//    @Override TODO hibernate search 6
//    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
//        Assert.notNull(queryBuilder);
//
//        Interval<IV> interval = getValue();
//        IV from = interval.getFrom();
//        IV to = interval.getTo();
//
//        return queryBuilder.range().onField(getAttributeName()).from(from).to(to).createQuery();
//    }
}
