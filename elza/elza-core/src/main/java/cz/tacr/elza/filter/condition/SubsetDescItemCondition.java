package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.springframework.util.Assert;

/**
 * Spadá do celého období.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class SubsetDescItemCondition<T extends Interval<IV>, IV> extends AbstractIntervalDescItemConditionWithValue<T, IV> {

    public SubsetDescItemCondition(T conditionValue, String attributeNameFrom, String attributeNameTo) {
        super(conditionValue, attributeNameFrom, attributeNameTo);
    }

    @Override
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        Assert.notNull(factory);

        Interval<IV> interval = getValue();
        IV from = interval.getFrom();
        IV to = interval.getTo();

        SearchPredicate fromPredicate = factory.range().field(getAttributeNameFrom()).atLeast(from).toPredicate();
        SearchPredicate toPredicate = factory.range().field(getAttributeNameTo()).atMost(to).toPredicate();

        return factory.bool().must(fromPredicate).must(toPredicate).toPredicate();
    }
}
