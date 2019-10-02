package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 * Spadá částečně do období.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class IntersectDescItemCondition<T extends Interval<IV>, IV> extends AbstractIntervalDescItemConditionWithValue<T, IV> {

    public IntersectDescItemCondition(T conditionValue, String attributeNameFrom, String attributeNameTo) {
        super(conditionValue, attributeNameFrom, attributeNameTo);
    }

    @Override
    public Query createLuceneQuery(QueryBuilder queryBuilder) {
        Assert.notNull(queryBuilder);

        Interval<IV> interval = getValue();
        IV from = interval.getFrom();
        IV to = interval.getTo();

        // conditionFrom < dataceFrom && conditionTo > dataceFrom && conditionTo < dataceTo
        Query fromQuery1 = queryBuilder.range().onField(getAttributeNameFrom()).from(from).to(to).createQuery();
        Query fromQuery2 = queryBuilder.range().onField(getAttributeNameTo()).above(to).createQuery();
        Query fromQuery = queryBuilder.bool().must(fromQuery1).must(fromQuery2).createQuery();

        //conditionFrom < dataceTo && conditionTo > dataceTo && conditionFrom > dataceFrom
        Query toQuery1 = queryBuilder.range().onField(getAttributeNameTo()).from(from).to(to).createQuery();
        Query toQuery2 = queryBuilder.range().onField(getAttributeNameFrom()).below(from).createQuery();
        Query toQuery = queryBuilder.bool().must(toQuery1).must(toQuery2).createQuery();

        return queryBuilder.bool().should(fromQuery).should(toQuery).createQuery();
    }
}
