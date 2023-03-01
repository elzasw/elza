package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

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

    @Override
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        SearchPredicate predicate = super.createLucenePredicate(factory);

        return factory.bool().mustNot(predicate).toPredicate();
    }
}
