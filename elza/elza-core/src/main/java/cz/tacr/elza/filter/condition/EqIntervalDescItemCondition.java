package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * 
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public class EqIntervalDescItemCondition<T extends Interval<IV>, IV> extends AbstractIntervalDescItemConditionWithValue<T, IV> {

    public EqIntervalDescItemCondition(final T conditionValue, final String attributeNameFrom, final String attributeNameTo) {
        super(conditionValue, attributeNameFrom, attributeNameTo);
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		Interval<IV> interval = getValue();
		IV from = interval.getFrom();
		IV to = interval.getTo();

		SearchPredicate fp = factory.bool().should(factory.range().field(getAttributeNameFrom()).between(from, from)).toPredicate();
		SearchPredicate tp = factory.bool().should(factory.range().field(getAttributeNameTo()).between(to, to)).toPredicate();

		return factory.bool().must(fp).must(tp).toPredicate();
	}
}
