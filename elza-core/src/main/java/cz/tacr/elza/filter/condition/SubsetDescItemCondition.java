package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Spadá do celého období.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public class SubsetDescItemCondition<T extends Interval<IV>, IV> extends AbstractIntervalDescItemConditionWithValue<T, IV> {

    public SubsetDescItemCondition(T conditionValue, String attributeNameFrom, String attributeNameTo) {
        super(conditionValue, attributeNameFrom, attributeNameTo);
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		Interval<IV> interval = getValue();
		IV from = interval.getFrom();
		IV to = interval.getTo();

		SearchPredicate fp = factory.bool().should(factory.range().field(getAttributeNameFrom()).atLeast(from)).toPredicate();
		SearchPredicate tp = factory.bool().should(factory.range().field(getAttributeNameTo()).atMost(to)).toPredicate();

		return factory.bool().must(fp).must(tp).toPredicate();
	}
//        Query fromQuery = queryBuilder.range().onField(getAttributeNameFrom()).above(from).createQuery();
//        Query toQuery = queryBuilder.range().onField(getAttributeNameTo()).below(to).createQuery();
//
//        return queryBuilder.bool().must(fromQuery).must(toQuery).createQuery();
}
