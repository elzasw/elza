package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Filtr pro podmínku větší nebo rovno.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public class GeDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public GeDescItemCondition(final T conditionValue, final String attributeName) {
        super(conditionValue, attributeName);
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		return factory.bool()
				.should(factory.range().field(getAttributeName()).atLeast(getValue()))
				.toPredicate();
	}
}
