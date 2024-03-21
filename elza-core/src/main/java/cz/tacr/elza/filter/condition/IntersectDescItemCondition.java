package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Spadá částečně do období.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public class IntersectDescItemCondition<T extends Interval<IV>, IV> extends AbstractIntervalDescItemConditionWithValue<T, IV> {

    public IntersectDescItemCondition(T conditionValue, String attributeNameFrom, String attributeNameTo) {
        super(conditionValue, attributeNameFrom, attributeNameTo);
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		Interval<IV> interval = getValue();
		IV from = interval.getFrom();
		IV to = interval.getTo();

      	// (StartA <= EndB) and (EndA >= StartB)
		// see https://stackoverflow.com/questions/325933/determine-whether-two-date-ranges-overlap
		SearchPredicate sp1 = factory.bool().should(factory.range().field(getAttributeNameFrom()).atMost(to)).toPredicate();
		SearchPredicate sp2 = factory.bool().should(factory.range().field(getAttributeNameTo()).atLeast(from)).toPredicate();

		return factory.bool().must(sp1).must(sp2).toPredicate();
	}
//        Query fromQuery1 = queryBuilder.range().onField(getAttributeNameFrom()).below(to).createQuery();
//        Query fromQuery2 = queryBuilder.range().onField(getAttributeNameTo()).above(from).createQuery();
//
//        return queryBuilder.bool().must(fromQuery1).must(fromQuery2).createQuery();
}
