package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Neobsahuje.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @param <T>
 * @since 14. 4. 2016
 */
public class NotContainDescItemCondition<T> extends ContainDescItemCondition<T>{

    public NotContainDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }

    @Override
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        SearchPredicate predicate = super.createLucenePredicate(factory);

        return factory.bool().mustNot(predicate).toPredicate();
    }
}
