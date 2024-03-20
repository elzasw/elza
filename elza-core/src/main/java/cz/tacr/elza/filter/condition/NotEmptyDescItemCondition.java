package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

import cz.tacr.elza.domain.ArrDescItem;

/**
 * Má vyplněnu nějakou hodnotu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class NotEmptyDescItemCondition implements LuceneDescItemCondition {

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		BooleanPredicateClausesStep<?> bool = factory.bool();
		
		return bool.should(factory.exists().field(ArrDescItem.FULLTEXT_ATT).toPredicate()).toPredicate();
		//return bool.should(factory.wildcard().field(ArrDescItem.FULLTEXT_ATT).matching("?*").toPredicate()).toPredicate();
	}

//    @Override TODO hibernate search 6
//    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
//        Assert.notNull(queryBuilder);
//
//		return queryBuilder.keyword().wildcard().onField(ArrDescItem.FULLTEXT_ATT).matching("?*").createQuery();
//    }
}
