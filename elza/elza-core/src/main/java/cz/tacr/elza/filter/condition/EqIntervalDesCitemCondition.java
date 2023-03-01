package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
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
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        Assert.notNull(factory);

        Interval<IV> interval = getValue();
        IV from = interval.getFrom();
        IV to = interval.getTo();

        SearchPredicate fromPredicate = factory.range().field(getAttributeNameFrom()).between(from, from).toPredicate();
        SearchPredicate toPredicate = factory.range().field(getAttributeNameTo()).between(to, to).toPredicate();

        return factory.bool().must(fromPredicate).must(toPredicate).toPredicate();
    }
}
