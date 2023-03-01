package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.springframework.util.Assert;

/**
 * Končí na.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class EndDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public EndDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }


    @Override
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        Assert.notNull(factory);

        return factory.wildcard().field(getAttributeName()).matching("*" + getValue()).toPredicate();
    }
}
