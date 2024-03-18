package cz.tacr.elza.filter.condition;

//import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
//import org.hibernate.search.query.dsl.QueryBuilder; TODO hibernate search 6
//import org.springframework.util.Assert;

/**
 * Začíná na.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class BeginDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public BeginDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override
//    public Query createLuceneQuery(QueryBuilder queryBuilder) { TODO hibernate search 6
//        Assert.notNull(queryBuilder);
//
//        return queryBuilder.keyword().wildcard().onField(getAttributeName()).matching(getValue() + "*").createQuery();
//    }
}
