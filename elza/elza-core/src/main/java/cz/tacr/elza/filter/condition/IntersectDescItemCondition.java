package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

//import org.apache.commons.lang3.Validate;
//import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder; TODO hibernate search 6
//import org.springframework.util.Assert;

/**
 * Spadá částečně do období.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class IntersectDescItemCondition<T extends Interval<IV>, IV> extends AbstractIntervalDescItemConditionWithValue<T, IV> {

    public IntersectDescItemCondition(T conditionValue, String attributeNameFrom, String attributeNameTo) {
        super(conditionValue, attributeNameFrom, attributeNameTo);
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override TODO hibernate search 6
//    public Query createLuceneQuery(QueryBuilder queryBuilder) {
//        Validate.notNull(queryBuilder);
//
//        Interval<IV> interval = getValue();
//        IV from = interval.getFrom();
//        IV to = interval.getTo();
//
//        // (StartA <= EndB) and (EndA >= StartB)
//        // see https://stackoverflow.com/questions/325933/determine-whether-two-date-ranges-overlap
//        Query fromQuery1 = queryBuilder.range().onField(getAttributeNameFrom()).below(to).createQuery();
//        Query fromQuery2 = queryBuilder.range().onField(getAttributeNameTo()).above(from).createQuery();
//
//        return queryBuilder.bool().must(fromQuery1).must(fromQuery2).createQuery();
//    }
}
