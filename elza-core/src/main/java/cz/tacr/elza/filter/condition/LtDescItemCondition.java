package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.springframework.util.Assert;

/**
 * Menší než.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class LtDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public LtDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }


    @Override
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        Assert.notNull(factory);

        return factory.range().field(getAttributeName()).lessThan(getValue()).toPredicate();
    }
}
