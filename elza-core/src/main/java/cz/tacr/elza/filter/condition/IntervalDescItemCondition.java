package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
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

    @Override
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        Assert.notNull(factory);

        Interval<IV> interval = getValue();
        IV from = interval.getFrom();
        IV to = interval.getTo();

        return factory.range().field(getAttributeName()).between(from, to).toPredicate();
    }
}
