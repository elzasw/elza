package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

//import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder; TODO hibernate search 6
//import org.springframework.util.Assert;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class GtDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public GtDescItemCondition(final T conditionValue, final String attributeName) {
        super(conditionValue, attributeName);
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override TODO hibernate search 6
//    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
//        Assert.notNull(queryBuilder);
//
//        return queryBuilder.range().onField(getAttributeName()).above(getValue()).excludeLimit().createQuery();
//    }

}
