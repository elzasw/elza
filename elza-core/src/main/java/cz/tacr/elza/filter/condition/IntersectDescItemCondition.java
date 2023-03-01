package cz.tacr.elza.filter.condition;

import org.apache.commons.lang3.Validate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

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
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        Validate.notNull(factory);

        Interval<IV> interval = getValue();
        IV from = interval.getFrom();
        IV to = interval.getTo();

        SearchPredicate predicate1 = factory.range().field(getAttributeNameFrom()).atMost(to).toPredicate();
        SearchPredicate predicate2 = factory.range().field(getAttributeNameTo()).atLeast(from).toPredicate();

        return factory.bool().must(predicate1).must(predicate2).toPredicate();
    }
}
