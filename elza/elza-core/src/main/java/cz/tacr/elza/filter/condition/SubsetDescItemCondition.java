package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

//import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder; TODO hibernate search 6
//import org.springframework.util.Assert;

/**
 * Spadá do celého období.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class SubsetDescItemCondition<T extends Interval<IV>, IV> extends AbstractIntervalDescItemConditionWithValue<T, IV> {

    public SubsetDescItemCondition(T conditionValue, String attributeNameFrom, String attributeNameTo) {
        super(conditionValue, attributeNameFrom, attributeNameTo);
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override TODO hibernate search 6
//    public Query createLuceneQuery(QueryBuilder queryBuilder) {
//        Assert.notNull(queryBuilder);
//
//        Interval<IV> interval = getValue();
//        IV from = interval.getFrom();
//        IV to = interval.getTo();
//
//        Query fromQuery = queryBuilder.range().onField(getAttributeNameFrom()).above(from).createQuery();
//        Query toQuery = queryBuilder.range().onField(getAttributeNameTo()).below(to).createQuery();
//
//        return queryBuilder.bool().must(fromQuery).must(toQuery).createQuery();
//    }

}
