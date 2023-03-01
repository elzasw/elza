package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.springframework.util.Assert;

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
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        Assert.notNull(factory);

        return factory.range().field(getAttributeName()).greaterThan(getValue()).toPredicate();
    }
}
