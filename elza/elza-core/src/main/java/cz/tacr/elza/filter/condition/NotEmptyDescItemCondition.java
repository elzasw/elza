package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

import cz.tacr.elza.domain.ArrDescItem;

/**
 * Má vyplněnu nějakou hodnotu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public class NotEmptyDescItemCondition implements LuceneDescItemCondition {

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		return factory.bool()
				.should(factory.exists().field(ArrDescItem.FULLTEXT_ATT).toPredicate())
				.toPredicate();
	}
}
