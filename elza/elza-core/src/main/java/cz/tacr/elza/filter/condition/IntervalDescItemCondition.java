package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.data.Range;

/**
 * Je v intervalu včetně.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public class IntervalDescItemCondition<T extends Interval<IV>, IV> extends AbstractDescItemConditionWithValue<T> {

    public IntervalDescItemCondition(final T conditionValue, final String attributeName) {
        super(conditionValue, attributeName);
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		Interval<IV> interval = getValue();
		IV from = interval.getFrom();
		IV to = interval.getTo();

		return factory.bool()
				.should(factory.range().field(getAttributeName()).range(Range.canonical(from, to)))
				.toPredicate();
	}
}
