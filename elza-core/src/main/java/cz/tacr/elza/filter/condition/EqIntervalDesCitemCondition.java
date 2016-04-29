package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 4. 2016
 */
public class EqIntervalDesCitemCondition<T extends Interval<IV>, IV> extends AbstractIntervalDescItemConditionWithValue<T, IV> {

    public EqIntervalDesCitemCondition(final T conditionValue, final String attributeNameFrom, final String attributeNameTo) {
        super(conditionValue, attributeNameFrom, attributeNameTo);
    }

    @Override
    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
        Assert.notNull(queryBuilder);

        Interval<IV> interval = getValue();
        IV from = interval.getFrom();
        IV to = interval.getTo();

        Query fromQuery = queryBuilder.range().onField(getAttributeNameFrom()).from(from).to(from).createQuery();
        Query toQuery = queryBuilder.range().onField(getAttributeNameTo()).from(to).to(to).createQuery();

        return queryBuilder.bool().must(fromQuery).must(toQuery).createQuery();
    }
}
