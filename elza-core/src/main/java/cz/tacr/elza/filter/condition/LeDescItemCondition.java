package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder; TODO hibernate search 6
import org.springframework.util.Assert;

/**
 * Menší nebo rovno než.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class LeDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public LeDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }

//    @Override
//    public Query createLuceneQuery(QueryBuilder queryBuilder) { TODO hibernate search 6
//        Assert.notNull(queryBuilder);
//
//        return queryBuilder.range().onField(getAttributeName()).below(getValue()).createQuery();
//    }
}
