package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

import cz.tacr.elza.domain.ArrDescItem;

/**
 * Podmínka pro výběr všech neprázdných hodnot specifikace id.
 *
 * @author Sergey Iryupin
 * @since 12. 2. 2025
 */
public class NotEmptySpecificationDescItemEnumCondition implements LuceneDescItemCondition {

	@Override
	public SearchPredicate createSearchPredicate(SearchPredicateFactory factory) {
		return factory.bool()
				.should(factory.exists().field(ArrDescItem.FIELD_ITEM_SPEC_ID))
				.toPredicate();
	}
}
