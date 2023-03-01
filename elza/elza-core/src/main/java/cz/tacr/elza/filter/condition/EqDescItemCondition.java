package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Rovno.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class EqDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public EqDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }

    @Override
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        return factory.match().field(getAttributeName()).matching(getValue()).skipAnalysis().toPredicate();
    }
}
